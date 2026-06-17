package com.example.Cricbook.service;

import com.example.Cricbook.dto.SplitPaymentDtos;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.CollectionStatus;
import com.example.Cricbook.enums.PaymentSplitStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SplitPaymentService {
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentSplitRepository splitRepository;
    private final TeamCollectionRepository collectionRepository;
    private final PaymentRuleRepository ruleRepository;
    private final TeamRepository teamRepository;
    private final RecurringCollectionRepository recurringRepository;
    private final PaymentSettlementRepository settlementRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    @Transactional
    public SplitPaymentDtos.SplitPaymentResponse createSplit(String username, SplitPaymentDtos.CreateSplitRequest request) {
        User captain = findUser(username);
        if (request == null || request.getBookingId() == null) {
            throw ApiException.badRequest("Booking is required to create a payment split.");
        }
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        validateCaptainAccess(booking, captain);
        if (!splitRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId()).isEmpty()) {
            throw ApiException.conflict("A split payment plan already exists for this booking.");
        }

        List<SplitPaymentDtos.SplitShareRequest> requestedShares = request.getShares() == null ? List.of() : request.getShares();
        List<User> players = requestedShares.isEmpty()
                ? List.of(booking.getUser())
                : requestedShares.stream().map(this::resolveShareUser).distinct().toList();
        if (players.isEmpty()) {
            throw ApiException.badRequest("Add at least one player to the split.");
        }

        Map<Long, BigDecimal> amounts = calculateAmounts(booking.getAmount(), players, requestedShares, request.getSplitType());
        LocalDate dueDate = request.getDueDate() == null ? booking.getBookingDate() : request.getDueDate();

        List<PaymentSplit> saved = players.stream()
                .map(player -> splitRepository.save(PaymentSplit.builder()
                        .booking(booking)
                        .user(player)
                        .amount(amounts.get(player.getId()))
                        .status(PaymentSplitStatus.PENDING)
                        .dueDate(dueDate)
                        .paymentLink("/split-payments?bookingId=" + booking.getId())
                        .build()))
                .toList();

        TeamCollection collection = collectionRepository.save(TeamCollection.builder()
                .booking(booking)
                .captain(captain)
                .totalAmount(booking.getAmount())
                .collectedAmount(BigDecimal.ZERO)
                .pendingAmount(booking.getAmount())
                .dueDate(dueDate)
                .status(CollectionStatus.OPEN)
                .build());

        saved.forEach(split -> notifySplit(split, "Payment Share Created", "Pay your Cricbook share of Rs " + split.getAmount() + " for booking #" + booking.getId() + "."));
        return buildResponse(booking.getId(), collection, saved);
    }

    @Transactional(readOnly = true)
    public SplitPaymentDtos.SplitPaymentResponse getBookingSplit(String username, Long bookingId) {
        User user = findUser(username);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        validateViewAccess(booking, user);
        return buildResponse(bookingId,
                collectionRepository.findByBookingId(bookingId).orElse(null),
                splitRepository.findByBookingIdOrderByCreatedAtAsc(bookingId));
    }

    @Transactional(readOnly = true)
    public List<SplitPaymentDtos.PaymentSplitResponse> mySplits(String username) {
        User user = findUser(username);
        return splitRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toSplitResponse)
                .toList();
    }

    @Transactional
    public SplitPaymentDtos.SplitPaymentResponse payShare(String username, SplitPaymentDtos.PayShareRequest request) {
        User user = findUser(username);
        if (request == null || request.getSplitId() == null) {
            throw ApiException.badRequest("Split share is required.");
        }
        PaymentSplit split = splitRepository.findById(request.getSplitId())
                .orElseThrow(() -> ApiException.notFound("Payment split was not found."));
        if (!split.getUser().getId().equals(user.getId()) && !isAdmin(user)) {
            throw ApiException.forbidden("You can only pay your own split share.");
        }
        if (split.getStatus() == PaymentSplitStatus.PAID) {
            return getBookingSplit(username, split.getBooking().getId());
        }
        if (Boolean.TRUE.equals(request.getUseWallet())) {
            walletService.debitWallet(user, split.getAmount(), "SPLIT_PAYMENT", "Paid split for booking #" + split.getBooking().getId());
        }
        split.setStatus(PaymentSplitStatus.PAID);
        split.setPaidAt(LocalDateTime.now());
        split.setPaymentReference(request.getPaymentReference() == null ? "SIMULATED_SPLIT_" + split.getId() : request.getPaymentReference());
        splitRepository.save(split);
        TeamCollection collection = recalculateCollection(split.getBooking().getId());
        notificationService.pushNotification(
                split.getBooking().getUser().getId(),
                split.getBooking().getUser().getEmail(),
                "Split Payment Received",
                split.getUser().getName() + " paid Rs " + split.getAmount() + " for booking #" + split.getBooking().getId() + "."
        );
        return buildResponse(split.getBooking().getId(), collection, splitRepository.findByBookingIdOrderByCreatedAtAsc(split.getBooking().getId()));
    }

    @Transactional
    public SplitPaymentDtos.SplitPaymentResponse sendReminders(String username, Long bookingId) {
        User user = findUser(username);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        validateCaptainAccess(booking, user);
        List<PaymentSplit> splits = splitRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        splits.stream()
                .filter(split -> split.getStatus() == PaymentSplitStatus.PENDING || split.getStatus() == PaymentSplitStatus.OVERDUE)
                .forEach(split -> notifySplit(split, "Payment Reminder", "Reminder: pay your Rs " + split.getAmount() + " share for booking #" + bookingId + "."));
        return buildResponse(bookingId, collectionRepository.findByBookingId(bookingId).orElse(null), splits);
    }

    @Transactional
    public SplitPaymentDtos.PaymentRuleResponse updateOwnerRules(String username, SplitPaymentDtos.PaymentRuleRequest request) {
        User owner = findUser(username);
        int minimum = clamp(request == null || request.getMinimumPercentage() == null ? 50 : request.getMinimumPercentage(), 0, 100);
        PaymentRule rule = ruleRepository.findByOwnerId(owner.getId()).orElseGet(() -> PaymentRule.builder()
                .owner(owner)
                .minimumPercentage(50)
                .autoCancel(false)
                .fullPaymentRequired(false)
                .reminderHoursBefore(24)
                .build());
        rule.setMinimumPercentage(minimum);
        rule.setAutoCancel(request == null || request.getAutoCancel() == null ? rule.getAutoCancel() : request.getAutoCancel());
        rule.setFullPaymentRequired(request == null || request.getFullPaymentRequired() == null ? rule.getFullPaymentRequired() : request.getFullPaymentRequired());
        rule.setReminderHoursBefore(request == null || request.getReminderHoursBefore() == null ? rule.getReminderHoursBefore() : Math.max(request.getReminderHoursBefore(), 1));
        return toRuleResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public SplitPaymentDtos.PaymentRuleResponse getOwnerRules(String username) {
        User owner = findUser(username);
        return toRuleResponse(ruleRepository.findByOwnerId(owner.getId()).orElseGet(() -> PaymentRule.builder()
                .owner(owner)
                .minimumPercentage(50)
                .autoCancel(false)
                .fullPaymentRequired(false)
                .reminderHoursBefore(24)
                .build()));
    }

    @Transactional
    public SplitPaymentDtos.RecurringCollectionResponse createRecurring(String username, SplitPaymentDtos.RecurringCollectionRequest request) {
        User user = findUser(username);
        if (request == null || request.getTeamId() == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Team and amount are required for recurring collection.");
        }
        Team team = teamRepository.findById(request.getTeamId()).orElseThrow(() -> ApiException.notFound("Team was not found."));
        if (!team.getCaptain().getId().equals(user.getId()) && !isAdmin(user)) {
            throw ApiException.forbidden("Only the team captain can create recurring collections.");
        }
        RecurringCollection saved = recurringRepository.save(RecurringCollection.builder()
                .team(team)
                .createdBy(user)
                .amount(request.getAmount())
                .frequency(request.getFrequency() == null ? "MONTHLY" : request.getFrequency())
                .active(true)
                .description(request.getDescription() == null ? "Team collection" : request.getDescription())
                .build());
        return toRecurringResponse(saved);
    }

    @Transactional(readOnly = true)
    public SplitPaymentDtos.SplitAnalyticsResponse analytics(String username) {
        User user = findUser(username);
        List<PaymentSplit> splits = isAdmin(user)
                ? splitRepository.findAll()
                : isOwner(user)
                    ? splitRepository.findByBooking_Turf_Owner_IdOrderByCreatedAtDesc(user.getId())
                    : splitRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return buildAnalytics(splits);
    }

    @Transactional
    public void createProportionalSettlements(Long bookingId) {
        List<PaymentSplit> paid = splitRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .filter(split -> split.getStatus() == PaymentSplitStatus.PAID)
                .toList();
        paid.forEach(split -> {
            split.setStatus(PaymentSplitStatus.REFUNDED);
            splitRepository.save(split);
            settlementRepository.save(PaymentSettlement.builder()
                    .booking(split.getBooking())
                    .user(split.getUser())
                    .refundAmount(split.getAmount())
                    .status("PENDING")
                    .build());
        });
        recalculateCollection(bookingId);
    }

    private Map<Long, BigDecimal> calculateAmounts(BigDecimal total, List<User> players, List<SplitPaymentDtos.SplitShareRequest> requestedShares, String splitType) {
        boolean custom = "CUSTOM".equalsIgnoreCase(splitType);
        Map<Long, BigDecimal> amounts = new HashMap<>();
        if (custom) {
            BigDecimal sum = BigDecimal.ZERO;
            for (SplitPaymentDtos.SplitShareRequest share : requestedShares) {
                User user = resolveShareUser(share);
                if (share.getAmount() == null || share.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw ApiException.badRequest("Custom split amounts must be greater than zero.");
                }
                amounts.put(user.getId(), share.getAmount());
                sum = sum.add(share.getAmount());
            }
            if (sum.compareTo(total) != 0) {
                throw ApiException.badRequest("Custom split amounts must equal the booking total.");
            }
            return amounts;
        }
        BigDecimal perPlayer = total.divide(BigDecimal.valueOf(players.size()), 2, RoundingMode.DOWN);
        BigDecimal assigned = BigDecimal.ZERO;
        for (int i = 0; i < players.size(); i++) {
            BigDecimal amount = i == players.size() - 1 ? total.subtract(assigned) : perPlayer;
            amounts.put(players.get(i).getId(), amount);
            assigned = assigned.add(amount);
        }
        return amounts;
    }

    private TeamCollection recalculateCollection(Long bookingId) {
        TeamCollection collection = collectionRepository.findByBookingId(bookingId)
                .orElseThrow(() -> ApiException.notFound("Collection was not found."));
        List<PaymentSplit> splits = splitRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        BigDecimal collected = splits.stream()
                .filter(split -> split.getStatus() == PaymentSplitStatus.PAID)
                .map(PaymentSplit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = splits.stream().map(PaymentSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        collection.setCollectedAmount(collected);
        collection.setPendingAmount(total.subtract(collected));
        collection.setStatus(collected.compareTo(total) == 0 ? CollectionStatus.COLLECTED : collected.compareTo(BigDecimal.ZERO) > 0 ? CollectionStatus.PARTIALLY_PAID : CollectionStatus.OPEN);
        return collectionRepository.save(collection);
    }

    private SplitPaymentDtos.SplitPaymentResponse buildResponse(Long bookingId, TeamCollection collection, List<PaymentSplit> splits) {
        return SplitPaymentDtos.SplitPaymentResponse.builder()
                .bookingId(bookingId)
                .collection(collection == null ? null : toCollectionResponse(collection, splits))
                .splits(splits.stream().map(this::toSplitResponse).toList())
                .analytics(buildAnalytics(splits))
                .build();
    }

    private SplitPaymentDtos.SplitAnalyticsResponse buildAnalytics(List<PaymentSplit> splits) {
        long paid = splits.stream().filter(split -> split.getStatus() == PaymentSplitStatus.PAID).count();
        long pending = splits.stream().filter(split -> split.getStatus() == PaymentSplitStatus.PENDING || split.getStatus() == PaymentSplitStatus.OVERDUE).count();
        BigDecimal collected = splits.stream().filter(split -> split.getStatus() == PaymentSplitStatus.PAID).map(PaymentSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingAmount = splits.stream().filter(split -> split.getStatus() != PaymentSplitStatus.PAID).map(PaymentSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int rate = splits.isEmpty() ? 0 : (int) Math.round((paid * 100.0) / splits.size());
        return SplitPaymentDtos.SplitAnalyticsResponse.builder()
                .totalSplits((long) splits.size())
                .paidSplits(paid)
                .pendingSplits(pending)
                .totalCollected(collected)
                .totalPending(pendingAmount)
                .collectionSuccessRate(rate)
                .insight(rate >= 80 ? "Collection health is strong." : "Send reminders or require higher upfront payment for this group.")
                .build();
    }

    private SplitPaymentDtos.TeamCollectionResponse toCollectionResponse(TeamCollection collection, List<PaymentSplit> splits) {
        long paid = splits.stream().filter(split -> split.getStatus() == PaymentSplitStatus.PAID).count();
        long pending = splits.size() - paid;
        int rate = splits.isEmpty() ? 0 : (int) Math.round((paid * 100.0) / splits.size());
        return SplitPaymentDtos.TeamCollectionResponse.builder()
                .id(collection.getId())
                .bookingId(collection.getBooking().getId())
                .captainId(collection.getCaptain().getId())
                .captainName(collection.getCaptain().getName())
                .totalAmount(collection.getTotalAmount())
                .collectedAmount(collection.getCollectedAmount())
                .pendingAmount(collection.getPendingAmount())
                .paidPlayers(paid)
                .pendingPlayers(pending)
                .collectionRate(rate)
                .dueDate(collection.getDueDate())
                .status(collection.getStatus())
                .build();
    }

    private SplitPaymentDtos.PaymentSplitResponse toSplitResponse(PaymentSplit split) {
        return SplitPaymentDtos.PaymentSplitResponse.builder()
                .id(split.getId())
                .bookingId(split.getBooking().getId())
                .turfName(split.getBooking().getTurf() == null ? null : split.getBooking().getTurf().getName())
                .bookingDate(split.getBooking().getBookingDate())
                .startTime(split.getBooking().getSlot() == null ? null : split.getBooking().getSlot().getStartTime())
                .endTime(split.getBooking().getSlot() == null ? null : split.getBooking().getSlot().getEndTime())
                .userId(split.getUser().getId())
                .playerName(split.getUser().getName())
                .amount(split.getAmount())
                .status(split.getStatus())
                .dueDate(split.getDueDate())
                .paidAt(split.getPaidAt())
                .paymentLink(split.getPaymentLink())
                .paymentReference(split.getPaymentReference())
                .build();
    }

    private SplitPaymentDtos.PaymentRuleResponse toRuleResponse(PaymentRule rule) {
        return SplitPaymentDtos.PaymentRuleResponse.builder()
                .id(rule.getId())
                .ownerId(rule.getOwner().getId())
                .minimumPercentage(rule.getMinimumPercentage())
                .autoCancel(rule.getAutoCancel())
                .fullPaymentRequired(rule.getFullPaymentRequired())
                .reminderHoursBefore(rule.getReminderHoursBefore())
                .build();
    }

    private SplitPaymentDtos.RecurringCollectionResponse toRecurringResponse(RecurringCollection recurring) {
        return SplitPaymentDtos.RecurringCollectionResponse.builder()
                .id(recurring.getId())
                .teamId(recurring.getTeam().getId())
                .amount(recurring.getAmount())
                .frequency(recurring.getFrequency())
                .active(recurring.getActive())
                .description(recurring.getDescription())
                .build();
    }

    private User resolveShareUser(SplitPaymentDtos.SplitShareRequest share) {
        if (share.getUserId() != null) {
            return userRepository.findById(share.getUserId()).orElseThrow(() -> ApiException.notFound("Split user was not found."));
        }
        if (share.getLoginId() != null && !share.getLoginId().isBlank()) {
            return userRepository.findByEmailOrPhone(share.getLoginId().trim()).orElseThrow(() -> ApiException.notFound("Split user was not found."));
        }
        throw ApiException.badRequest("Each split share needs a user.");
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username).orElseThrow(() -> ApiException.notFound("User was not found."));
    }

    private void validateCaptainAccess(Booking booking, User user) {
        if (!booking.getUser().getId().equals(user.getId()) && !isAdmin(user)) {
            throw ApiException.forbidden("Only the booking captain can manage this split.");
        }
    }

    private void validateViewAccess(Booking booking, User user) {
        boolean bookingCaptain = booking.getUser().getId().equals(user.getId());
        boolean splitParticipant = splitRepository.findByBookingIdAndUserId(booking.getId(), user.getId()).isPresent();
        boolean turfOwner = booking.getTurf() != null && booking.getTurf().getOwner() != null && booking.getTurf().getOwner().getId().equals(user.getId());
        if (!bookingCaptain && !splitParticipant && !turfOwner && !isAdmin(user)) {
            throw ApiException.forbidden("You do not have access to this collection.");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName() == com.example.Cricbook.enums.RoleName.ROLE_ADMIN);
    }

    private boolean isOwner(User user) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName() == com.example.Cricbook.enums.RoleName.ROLE_OWNER);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void notifySplit(PaymentSplit split, String title, String message) {
        notificationService.pushNotification(split.getUser().getId(), split.getUser().getEmail(), title, message);
    }
}
