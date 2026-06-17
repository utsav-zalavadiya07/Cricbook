package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResaleService {
    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.05);
    private static final int DEFAULT_TRUST_SCORE = 85;

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ResaleListingRepository listingRepository;
    private final BookingTransferRepository transferRepository;
    private final ResaleRuleRepository ruleRepository;
    private final SlotWaitlistRepository waitlistRepository;
    private final ResaleTransactionRepository transactionRepository;
    private final NotificationService notificationService;
    @Lazy
    private final TrustScoreService trustScoreService;

    @Transactional
    public ResaleListingResponse createListing(String loginId, CreateResaleListingRequest request) {
        if (request == null || request.getBookingId() == null || request.getPrice() == null) {
            throw ApiException.badRequest("Booking and resale price are required.");
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Resale price must be greater than zero.");
        }

        User seller = findUser(loginId);
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        if (!booking.getUser().getId().equals(seller.getId())) {
            throw ApiException.forbidden("You can only list your own booking for resale.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.conflict("Only confirmed bookings can be listed for resale.");
        }
        if (listingRepository.findByBookingIdAndStatus(booking.getId(), ResaleListingStatus.ACTIVE).isPresent()) {
            throw ApiException.conflict("This booking already has an active resale listing.");
        }

        ResaleRule rule = ensureRule(booking.getTurf().getOwner());
        if (!Boolean.TRUE.equals(rule.getAllowResale())) {
            throw ApiException.forbidden("This turf owner has disabled slot resale.");
        }
        validateTiming(booking, rule);
        validatePrice(request.getPrice(), booking.getAmount(), rule);
        validateTrust(seller.getId(), 40, "Your trust score is too low to list slots for resale.");

        LocalDateTime expiresAt = request.getExpiresAt() == null
                ? LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getStartTime()).minusMinutes(15)
                : request.getExpiresAt();
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Listing expiry must be in the future.");
        }

        ResaleListing listing = listingRepository.save(ResaleListing.builder()
                .booking(booking)
                .seller(seller)
                .price(request.getPrice())
                .originalAmount(booking.getAmount())
                .commissionAmount(commission(request.getPrice()))
                .status(ResaleListingStatus.ACTIVE)
                .expiresAt(expiresAt)
                .notes(clean(request.getNotes()))
                .build());

        notifyWaitlist(listing);
        return mapListing(listing, seller);
    }

    @Transactional(readOnly = true)
    public List<ResaleListingResponse> activeListings(String city) {
        User current = currentUserOrNull();
        return listingRepository.findActiveMarketplace(clean(city)).stream()
                .filter(listing -> listing.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(listing -> mapListing(listing, current))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResaleListingResponse> recommendations(String loginId) {
        User user = findUser(loginId);
        String preferredCity = user.getTenantId() == null ? null : null;
        return listingRepository.findActiveMarketplace(preferredCity).stream()
                .filter(listing -> listing.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(listing -> mapListing(listing, user))
                .sorted(Comparator.comparing(ResaleListingResponse::getRecommendationScore).reversed())
                .limit(12)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResaleListingResponse getListing(Long listingId) {
        ResaleListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Resale listing was not found."));
        return mapListing(listing, currentUserOrNull());
    }

    @Transactional
    public ResaleListingResponse buyListing(String loginId, Long listingId, BuyResaleListingRequest request) {
        User buyer = findUser(loginId);
        ResaleListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Resale listing was not found."));
        if (listing.getStatus() != ResaleListingStatus.ACTIVE) {
            throw ApiException.conflict("This resale listing is no longer available.");
        }
        if (!listing.getExpiresAt().isAfter(LocalDateTime.now())) {
            listing.setStatus(ResaleListingStatus.EXPIRED);
            listingRepository.save(listing);
            throw ApiException.conflict("This resale listing has expired.");
        }
        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw ApiException.badRequest("You cannot buy your own resale listing.");
        }

        ResaleRule rule = ensureRule(listing.getBooking().getTurf().getOwner());
        if (Boolean.TRUE.equals(rule.getRestrictLowTrustUsers())) {
            validateTrust(buyer.getId(), 60, "Your trust score does not meet this owner's transfer rule.");
        } else {
            validateTrust(buyer.getId(), 40, "Your trust score is too low to buy resale slots.");
        }
        if (transactionRepository.existsByListingId(listingId)) {
            throw ApiException.conflict("Payment is already being processed for this listing.");
        }

        Booking booking = listing.getBooking();
        User seller = listing.getSeller();
        booking.setUser(buyer);
        listing.setBuyer(buyer);
        listing.setStatus(ResaleListingStatus.SOLD);
        listing.setSoldAt(LocalDateTime.now());

        bookingRepository.save(booking);
        ResaleListing saved = listingRepository.save(listing);
        transactionRepository.save(ResaleTransaction.builder()
                .listing(saved)
                .buyer(buyer)
                .seller(seller)
                .amount(saved.getPrice())
                .commission(saved.getCommissionAmount())
                .status(ResaleTransactionStatus.RELEASED)
                .paymentReference(request == null ? null : clean(request.getPaymentReference()))
                .build());
        transferRepository.save(BookingTransfer.builder()
                .booking(booking)
                .fromUser(seller)
                .toUser(buyer)
                .status(BookingTransferStatus.ACCEPTED)
                .transferType("PUBLIC_MARKETPLACE")
                .acceptedAt(LocalDateTime.now())
                .build());

        notificationService.pushNotification(seller.getId(), seller.getEmail(), "Slot Sold", "Your Cricbook slot resale is complete.");
        notificationService.pushNotification(buyer.getId(), buyer.getEmail(), "Slot Transfer Complete", "The slot is now available in your bookings.");
        return mapListing(saved, buyer);
    }

    @Transactional
    public BookingTransferResponse createDirectTransfer(String loginId, TransferBookingRequest request) {
        if (request == null || request.getBookingId() == null || request.getToUserId() == null) {
            throw ApiException.badRequest("Booking and recipient are required for transfer.");
        }
        User fromUser = findUser(loginId);
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> ApiException.notFound("Transfer recipient was not found."));
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        if (!booking.getUser().getId().equals(fromUser.getId())) {
            throw ApiException.forbidden("You can only transfer your own booking.");
        }
        if (fromUser.getId().equals(toUser.getId())) {
            throw ApiException.badRequest("Recipient must be a different user.");
        }
        ResaleRule rule = ensureRule(booking.getTurf().getOwner());
        validateTiming(booking, rule);
        validateTrust(toUser.getId(), Boolean.TRUE.equals(rule.getRestrictLowTrustUsers()) ? 60 : 40, "Recipient does not meet the trust requirement for transfer.");

        booking.setUser(toUser);
        bookingRepository.save(booking);
        BookingTransfer transfer = transferRepository.save(BookingTransfer.builder()
                .booking(booking)
                .fromUser(fromUser)
                .toUser(toUser)
                .status(BookingTransferStatus.ACCEPTED)
                .transferType(clean(request.getTransferType()) == null ? "DIRECT_FRIEND" : clean(request.getTransferType()))
                .acceptedAt(LocalDateTime.now())
                .build());
        notificationService.pushNotification(toUser.getId(), toUser.getEmail(), "Booking Transferred", "A Cricbook slot was transferred to your account.");
        return mapTransfer(transfer);
    }

    @Transactional
    public SlotWaitlistResponse joinWaitlist(String loginId, JoinWaitlistRequest request) {
        if (request == null || request.getSlotId() == null || request.getDate() == null) {
            throw ApiException.badRequest("Slot and date are required to join waitlist.");
        }
        User user = findUser(loginId);
        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> ApiException.notFound("Slot was not found."));
        SlotWaitlist waitlist = waitlistRepository.findBySlotIdAndUserIdAndDate(slot.getId(), user.getId(), request.getDate())
                .orElse(SlotWaitlist.builder()
                        .slot(slot)
                        .user(user)
                        .date(request.getDate())
                        .build());
        waitlist.setCity(clean(request.getCity()) == null ? slot.getTurf().getCity() : clean(request.getCity()));
        waitlist.setPriority(request.getPriority() == null ? 1 : Math.max(1, Math.min(10, request.getPriority())));
        waitlist.setActive(true);
        return mapWaitlist(waitlistRepository.save(waitlist));
    }

    @Transactional(readOnly = true)
    public List<SlotWaitlistResponse> myWaitlist(String loginId) {
        User user = findUser(loginId);
        return waitlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapWaitlist)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResaleListingResponse> myListings(String loginId) {
        User user = findUser(loginId);
        return listingRepository.findBySellerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(listing -> mapListing(listing, user))
                .toList();
    }

    @Transactional
    public ResaleRuleResponse updateOwnerRules(String loginId, ResaleRuleRequest request) {
        User owner = findUser(loginId);
        ResaleRule rule = ensureRule(owner);
        if (request != null) {
            if (request.getAllowResale() != null) rule.setAllowResale(request.getAllowResale());
            if (request.getAutoApproveTransfer() != null) rule.setAutoApproveTransfer(request.getAutoApproveTransfer());
            if (request.getRestrictLowTrustUsers() != null) rule.setRestrictLowTrustUsers(request.getRestrictLowTrustUsers());
            if (request.getMaxMarkupPercent() != null) rule.setMaxMarkupPercent(Math.max(0, Math.min(50, request.getMaxMarkupPercent())));
            if (request.getMinHoursBeforeStart() != null) rule.setMinHoursBeforeStart(Math.max(1, Math.min(72, request.getMinHoursBeforeStart())));
        }
        return mapRule(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public ResaleRuleResponse getOwnerRules(String loginId) {
        return mapRule(ensureRule(findUser(loginId)));
    }

    @Transactional(readOnly = true)
    public ResaleAnalyticsResponse ownerAnalytics(String loginId) {
        User owner = findUser(loginId);
        List<ResaleListing> listings = listingRepository.findByOwnerId(owner.getId());
        return analyticsFrom(listings, owner);
    }

    @Transactional(readOnly = true)
    public ResaleAnalyticsResponse platformAnalytics() {
        return analyticsFrom(listingRepository.findAll(), null);
    }

    private ResaleAnalyticsResponse analyticsFrom(List<ResaleListing> listings, User viewer) {
        long active = listings.stream().filter(item -> item.getStatus() == ResaleListingStatus.ACTIVE).count();
        long sold = listings.stream().filter(item -> item.getStatus() == ResaleListingStatus.SOLD).count();
        long expired = listings.stream().filter(item -> item.getStatus() == ResaleListingStatus.EXPIRED).count();
        BigDecimal gmv = listings.stream()
                .filter(item -> item.getStatus() == ResaleListingStatus.SOLD)
                .map(ResaleListing::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long total = active + sold + expired;
        int successRate = total == 0 ? 0 : (int) Math.round((sold * 100.0) / total);
        return ResaleAnalyticsResponse.builder()
                .activeListings(active)
                .soldListings(sold)
                .expiredListings(expired)
                .waitlistUsers(waitlistRepository.countByActiveTrue())
                .marketplaceGmv(gmv)
                .recoveredRevenue(gmv)
                .successRate(successRate)
                .recentListings(listings.stream()
                        .sorted(Comparator.comparing(ResaleListing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(10)
                        .map(listing -> mapListing(listing, viewer))
                        .toList())
                .build();
    }

    private void validateTiming(Booking booking, ResaleRule rule) {
        LocalDateTime start = LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getStartTime());
        long hours = Duration.between(LocalDateTime.now(), start).toHours();
        if (hours < rule.getMinHoursBeforeStart()) {
            throw ApiException.conflict("This slot is too close to start time for resale or transfer.");
        }
    }

    private void validatePrice(BigDecimal price, BigDecimal original, ResaleRule rule) {
        BigDecimal multiplier = BigDecimal.ONE.add(BigDecimal.valueOf(rule.getMaxMarkupPercent()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        BigDecimal max = original.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        if (price.compareTo(max) > 0) {
            throw ApiException.badRequest("Resale price cannot exceed the owner markup rule.");
        }
    }

    private void validateTrust(Long userId, int minimum, String message) {
        if (trustScore(userId) < minimum) {
            throw ApiException.forbidden(message);
        }
    }

    private void notifyWaitlist(ResaleListing listing) {
        waitlistRepository.findBySlotIdAndDateAndActiveTrueOrderByPriorityDescCreatedAtAsc(
                listing.getBooking().getSlot().getId(),
                listing.getBooking().getBookingDate()
        ).forEach(waitlist -> notificationService.pushNotification(
                waitlist.getUser().getId(),
                waitlist.getUser().getEmail(),
                "Preferred Slot Available",
                "A waitlisted Cricbook slot is now available in the marketplace."
        ));
    }

    private ResaleRule ensureRule(User owner) {
        return ruleRepository.findByOwnerId(owner.getId())
                .orElseGet(() -> ResaleRule.builder()
                        .owner(owner)
                        .allowResale(true)
                        .autoApproveTransfer(true)
                        .restrictLowTrustUsers(true)
                        .maxMarkupPercent(0)
                        .minHoursBeforeStart(2)
                        .build());
    }

    private BigDecimal commission(BigDecimal price) {
        return price.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private ResaleListingResponse mapListing(ResaleListing listing, User viewer) {
        int sellerTrust = trustScore(listing.getSeller().getId());
        int buyerTrust = listing.getBuyer() == null ? 0 : trustScore(listing.getBuyer().getId());
        int recommendationScore = recommendationScore(listing, viewer, sellerTrust);
        return ResaleListingResponse.builder()
                .id(listing.getId())
                .bookingId(listing.getBooking().getId())
                .sellerId(listing.getSeller().getId())
                .sellerName(listing.getSeller().getName())
                .buyerId(listing.getBuyer() == null ? null : listing.getBuyer().getId())
                .buyerName(listing.getBuyer() == null ? null : listing.getBuyer().getName())
                .turfId(listing.getBooking().getTurf().getId())
                .turfName(listing.getBooking().getTurf().getName())
                .city(listing.getBooking().getTurf().getCity())
                .location(listing.getBooking().getTurf().getLocation())
                .slotId(listing.getBooking().getSlot().getId())
                .bookingDate(listing.getBooking().getBookingDate())
                .startTime(listing.getBooking().getSlot().getStartTime())
                .endTime(listing.getBooking().getSlot().getEndTime())
                .price(listing.getPrice())
                .originalAmount(listing.getOriginalAmount())
                .commissionAmount(listing.getCommissionAmount())
                .status(listing.getStatus())
                .expiresAt(listing.getExpiresAt())
                .soldAt(listing.getSoldAt())
                .notes(listing.getNotes())
                .sellerTrustScore(sellerTrust)
                .buyerTrustScore(buyerTrust)
                .recommendationScore(recommendationScore)
                .recommendationReason(recommendationReason(listing, viewer, recommendationScore))
                .createdAt(listing.getCreatedAt())
                .build();
    }

    private int recommendationScore(ResaleListing listing, User viewer, int sellerTrust) {
        int score = 45;
        if (sellerTrust >= 80) score += 20;
        if (listing.getPrice().compareTo(listing.getOriginalAmount()) <= 0) score += 15;
        long hoursLeft = Duration.between(LocalDateTime.now(), LocalDateTime.of(listing.getBooking().getBookingDate(), listing.getBooking().getSlot().getStartTime())).toHours();
        if (hoursLeft <= 24) score += 10;
        if (viewer != null && listing.getSeller().getId().equals(viewer.getId())) score -= 30;
        return Math.max(0, Math.min(100, score));
    }

    private String recommendationReason(ResaleListing listing, User viewer, int score) {
        if (viewer != null && listing.getSeller().getId().equals(viewer.getId())) {
            return "Your active resale listing.";
        }
        if (score >= 80) {
            return "High-fit slot based on timing, price, and seller reliability.";
        }
        if (listing.getPrice().compareTo(listing.getOriginalAmount()) < 0) {
            return "Discounted slot available before match time.";
        }
        return "Live marketplace slot available for transfer.";
    }

    private BookingTransferResponse mapTransfer(BookingTransfer transfer) {
        return BookingTransferResponse.builder()
                .id(transfer.getId())
                .bookingId(transfer.getBooking().getId())
                .fromUserId(transfer.getFromUser().getId())
                .fromUserName(transfer.getFromUser().getName())
                .toUserId(transfer.getToUser() == null ? null : transfer.getToUser().getId())
                .toUserName(transfer.getToUser() == null ? null : transfer.getToUser().getName())
                .status(transfer.getStatus())
                .transferType(transfer.getTransferType())
                .acceptedAt(transfer.getAcceptedAt())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    private SlotWaitlistResponse mapWaitlist(SlotWaitlist waitlist) {
        return SlotWaitlistResponse.builder()
                .id(waitlist.getId())
                .slotId(waitlist.getSlot().getId())
                .turfId(waitlist.getSlot().getTurf().getId())
                .turfName(waitlist.getSlot().getTurf().getName())
                .city(waitlist.getCity())
                .date(waitlist.getDate())
                .startTime(waitlist.getSlot().getStartTime())
                .endTime(waitlist.getSlot().getEndTime())
                .priority(waitlist.getPriority())
                .active(waitlist.getActive())
                .createdAt(waitlist.getCreatedAt())
                .build();
    }

    private ResaleRuleResponse mapRule(ResaleRule rule) {
        return ResaleRuleResponse.builder()
                .id(rule.getId())
                .ownerId(rule.getOwner().getId())
                .allowResale(rule.getAllowResale())
                .autoApproveTransfer(rule.getAutoApproveTransfer())
                .restrictLowTrustUsers(rule.getRestrictLowTrustUsers())
                .maxMarkupPercent(rule.getMaxMarkupPercent())
                .minHoursBeforeStart(rule.getMinHoursBeforeStart())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private int trustScore(Long userId) {
        try {
            TrustScoreResponse profile = trustScoreService == null ? null : trustScoreService.getProfile(userId);
            return profile == null || profile.getScore() == null ? DEFAULT_TRUST_SCORE : profile.getScore();
        } catch (RuntimeException ignored) {
            return DEFAULT_TRUST_SCORE;
        }
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private User currentUserOrNull() {
        try {
            String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
            return findUser(loginId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
