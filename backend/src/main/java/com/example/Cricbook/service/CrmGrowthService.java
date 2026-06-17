package com.example.Cricbook.service;

import com.example.Cricbook.entity.AuditLog;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.entity.Coupon;
import com.example.Cricbook.entity.CouponUsage;
import com.example.Cricbook.entity.LoyaltyPoint;
import com.example.Cricbook.entity.MarketingCampaign;
import com.example.Cricbook.entity.Payment;
import com.example.Cricbook.entity.Referral;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.UserMembership;
import com.example.Cricbook.entity.Wallet;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.CampaignStatus;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.AuditLogRepository;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.CouponRepository;
import com.example.Cricbook.repository.CouponUsageRepository;
import com.example.Cricbook.repository.LoyaltyPointRepository;
import com.example.Cricbook.repository.MarketingCampaignRepository;
import com.example.Cricbook.repository.PaymentRepository;
import com.example.Cricbook.repository.ReferralRepository;
import com.example.Cricbook.repository.UserMembershipRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.WalletRepository;
import com.example.Cricbook.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmGrowthService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;
    private final ReferralRepository referralRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MarketingCampaignRepository marketingCampaignRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> playerDashboard(String username) {
        User user = findUser(username);
        List<Booking> bookings = bookingRepository.findByUserId(user.getId());
        List<Payment> payments = paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(user.getId());
        List<Referral> referrals = referralRepository.findByReferrerId(user.getId());
        List<UserMembership> memberships = userMembershipRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Optional<Wallet> wallet = walletRepository.findByUserId(user.getId());
        int points = loyaltyPointRepository.findByUserId(user.getId()).map(LoyaltyPoint::getPoints).orElse(0);

        BigDecimal lifetimeSpend = successfulPaymentAmount(payments);
        long completedBookings = bookings.stream().filter(this::countsAsRetainedBooking).count();
        long cancelledBookings = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REFUNDED).count();
        long completedReferrals = referrals.stream().filter(this::isCompletedReferral).count();
        UserMembership activeMembership = memberships.stream()
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .filter(membership -> membership.getEndsAt() == null || membership.getEndsAt().isAfter(LocalDateTime.now()))
                .max(Comparator.comparing(UserMembership::getEndsAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("customer", Map.of(
                "id", user.getId(),
                "name", safe(user.getName()),
                "email", safe(user.getEmail()),
                "status", user.getStatus() == null ? "UNKNOWN" : user.getStatus().name()
        ));
        response.put("loyalty", Map.of(
                "pointsBalance", points,
                "walletEquivalent", pointsToWalletValue(points),
                "earnedPoints", Math.max(points, 0),
                "redeemable", points >= 10,
                "nextMilestonePoints", nextMilestone(points)
        ));
        Map<String, Object> membershipPayload = new LinkedHashMap<>();
        membershipPayload.put("active", activeMembership != null);
        membershipPayload.put("planName", activeMembership == null ? "No active plan" : activeMembership.getPlan().getName());
        membershipPayload.put("tier", growthTier(completedBookings, lifetimeSpend));
        membershipPayload.put("endsAt", activeMembership == null ? "" : activeMembership.getEndsAt());
        membershipPayload.put("benefits", activeMembership == null ? List.of("Earn loyalty points", "Referral rewards", "Wallet cashback")
                : List.of(
                activeMembership.getPlan().getDiscountPercent() + "% booking discount",
                Boolean.TRUE.equals(activeMembership.getPlan().getPrioritySlots()) ? "Priority booking access" : "Standard booking access",
                Boolean.TRUE.equals(activeMembership.getPlan().getFeaturedListing()) ? "Featured turf listing" : "Player growth benefits"
        ));
        response.put("membership", membershipPayload);
        response.put("wallet", Map.of(
                "balance", wallet.map(Wallet::getBalance).orElse(BigDecimal.ZERO),
                "transactions", wallet.map(value -> walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(value.getId()).size()).orElse(0)
        ));
        response.put("referrals", Map.of(
                "code", referralCode(user),
                "total", referrals.size(),
                "completed", completedReferrals,
                "pending", Math.max(referrals.size() - completedReferrals, 0)
        ));
        Map<String, Object> crmPayload = new LinkedHashMap<>();
        crmPayload.put("customerLifetimeValue", lifetimeSpend);
        crmPayload.put("projectedClv", projectedClv(lifetimeSpend, bookings));
        crmPayload.put("completedBookings", completedBookings);
        crmPayload.put("cancelledBookings", cancelledBookings);
        crmPayload.put("repeatCustomer", completedBookings > 1);
        crmPayload.put("lastBookingDate", lastBookingDate(bookings) == null ? "" : lastBookingDate(bookings));
        crmPayload.put("reEngagementStage", reEngagementStage(bookings));
        crmPayload.put("vipQualified", isVip(completedBookings, lifetimeSpend));
        response.put("crm", crmPayload);
        response.put("offers", playerOffers(points, activeMembership, bookings, referrals));
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        List<User> users = userRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();
        List<LoyaltyPoint> loyaltyPoints = loyaltyPointRepository.findAll();
        List<Referral> referrals = referralRepository.findAll();
        List<Coupon> coupons = couponRepository.findAll();
        List<CouponUsage> couponUsages = couponUsageRepository.findAll();
        List<UserMembership> memberships = userMembershipRepository.findAll();
        List<Wallet> wallets = walletRepository.findAll();
        List<MarketingCampaign> campaigns = marketingCampaignRepository.findAllByOrderByCreatedAtDesc();

        long bookers = bookings.stream().map(booking -> booking.getUser().getId()).distinct().count();
        long repeatBookers = repeatBookerCount(bookings);
        BigDecimal membershipRevenue = membershipRevenue(memberships);
        BigDecimal totalRevenue = successfulPaymentAmount(payments);
        BigDecimal walletLiability = wallets.stream().map(Wallet::getBalance).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        int loyaltyLiabilityPoints = loyaltyPoints.stream().map(LoyaltyPoint::getPoints).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("scorecard", Map.of(
                "totalCustomers", users.size(),
                "activeCustomers", users.stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).count(),
                "repeatCustomers", repeatBookers,
                "vipCustomers", vipCustomers(users, bookings, payments).size(),
                "inactiveCustomers", inactiveCustomers(users, bookings, 30).size(),
                "customerLifetimeValue", totalRevenue,
                "averageClv", users.isEmpty() ? BigDecimal.ZERO : totalRevenue.divide(BigDecimal.valueOf(users.size()), 2, RoundingMode.HALF_UP)
        ));
        response.put("loyalty", Map.of(
                "accounts", loyaltyPoints.size(),
                "pointsOutstanding", loyaltyLiabilityPoints,
                "walletEquivalentLiability", pointsToWalletValue(loyaltyLiabilityPoints),
                "redeemableAccounts", loyaltyPoints.stream().filter(point -> point.getPoints() != null && point.getPoints() >= 10).count()
        ));
        response.put("memberships", Map.of(
                "active", memberships.stream().filter(this::isActiveMembership).count(),
                "expired", memberships.stream().filter(membership -> membership.getStatus() == MembershipStatus.EXPIRED).count(),
                "revenue", membershipRevenue,
                "renewalRisk", memberships.stream().filter(this::expiresWithinSevenDays).count()
        ));
        response.put("referrals", Map.of(
                "total", referrals.size(),
                "completed", referrals.stream().filter(this::isCompletedReferral).count(),
                "conversionRate", percent(referrals.stream().filter(this::isCompletedReferral).count(), Math.max(referrals.size(), 1)),
                "pending", referrals.stream().filter(referral -> "PENDING".equalsIgnoreCase(referral.getStatus())).count()
        ));
        response.put("coupons", Map.of(
                "active", coupons.stream().filter(coupon -> Boolean.TRUE.equals(coupon.getActive())).count(),
                "usageCount", couponUsages.size(),
                "usageRate", percent(couponUsages.size(), Math.max(coupons.size(), 1)),
                "discountIssued", couponUsages.stream().map(CouponUsage::getDiscountAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
        ));
        response.put("wallet", Map.of(
                "wallets", wallets.size(),
                "liability", walletLiability,
                "averageBalance", wallets.isEmpty() ? BigDecimal.ZERO : walletLiability.divide(BigDecimal.valueOf(wallets.size()), 2, RoundingMode.HALF_UP)
        ));
        response.put("campaigns", Map.of(
                "total", campaigns.size(),
                "scheduled", campaigns.stream().filter(campaign -> campaign.getStatus() == CampaignStatus.SCHEDULED).count(),
                "sent", campaigns.stream().filter(campaign -> campaign.getStatus() == CampaignStatus.SENT).count(),
                "audienceReached", campaigns.stream().map(MarketingCampaign::getSentCount).filter(Objects::nonNull).mapToLong(Integer::longValue).sum()
        ));
        response.put("retention", Map.of(
                "repeatBookingRate", percent(repeatBookers, Math.max(bookers, 1)),
                "day30Inactive", inactiveCustomers(users, bookings, 30).size(),
                "day60Inactive", inactiveCustomers(users, bookings, 60).size(),
                "day90Inactive", inactiveCustomers(users, bookings, 90).size()
        ));
        response.put("segments", buildSegments(users, bookings, payments, memberships));
        response.put("insights", growthInsights(repeatBookers, bookers, referrals, couponUsages, memberships, campaigns));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> segments() {
        return buildSegments(userRepository.findAll(), bookingRepository.findAll(), paymentRepository.findAll(), userMembershipRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> automationPlaybooks() {
        List<User> users = userRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<UserMembership> memberships = userMembershipRepository.findAll();
        long inactive30 = inactiveCustomers(users, bookings, 30).size();
        long inactive60 = inactiveCustomers(users, bookings, 60).size();
        long inactive90 = inactiveCustomers(users, bookings, 90).size();
        long expiringMemberships = memberships.stream().filter(this::expiresWithinSevenDays).count();
        return List.of(
                automation("WELCOME_SERIES", "New customer onboarding", users.stream().filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))).count(), "Send first-booking coupon and turf recommendations."),
                automation("RE_ENGAGE_30", "No booking for 30 days", inactive30, "Send weekend coupon and favorite turf reminder."),
                automation("RE_ENGAGE_60", "No booking for 60 days", inactive60, "Send membership discount and referral bonus."),
                automation("RE_ENGAGE_90", "No booking for 90 days", inactive90, "Escalate to win-back campaign with wallet credit."),
                automation("MEMBERSHIP_RENEWAL", "Membership expiring in 7 days", expiringMemberships, "Send renewal reminder and downgrade-prevention offer."),
                automation("VIP_UPGRADE", "High CLV customer upgrade", vipCustomers(users, bookings, paymentRepository.findAll()).size(), "Offer VIP tier benefits and tournament access.")
        );
    }

    @Transactional
    public Map<String, Object> reconciliation() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<LoyaltyPoint> loyaltyPoints = loyaltyPointRepository.findAll();
        List<Wallet> wallets = walletRepository.findAll();
        List<CouponUsage> couponUsages = couponUsageRepository.findAll();
        List<Referral> referrals = referralRepository.findAll();
        List<UserMembership> memberships = userMembershipRepository.findAll();

        List<Map<String, Object>> checks = List.of(
                reconciliationCheck("Loyalty accounts linked to users", loyaltyPoints.size(), loyaltyPoints.stream().filter(point -> point.getUser() == null).count()),
                reconciliationCheck("Wallet balances non-negative", wallets.size(), wallets.stream().filter(wallet -> wallet.getBalance() == null || wallet.getBalance().compareTo(BigDecimal.ZERO) < 0).count()),
                reconciliationCheck("Coupon usage linked to coupon and user", couponUsages.size(), couponUsages.stream().filter(usage -> usage.getCoupon() == null || usage.getUser() == null).count()),
                reconciliationCheck("Referral status recognized", referrals.size(), referrals.stream().filter(referral -> !Set.of("PENDING", "COMPLETED", "REJECTED", "BLOCKED").contains(safe(referral.getStatus()).toUpperCase(Locale.ROOT))).count()),
                reconciliationCheck("Membership date windows valid", memberships.size(), memberships.stream().filter(membership -> membership.getStartsAt() != null && membership.getEndsAt() != null && membership.getEndsAt().isBefore(membership.getStartsAt())).count())
        );
        long mismatches = checks.stream().map(check -> ((Number) check.get("mismatches")).longValue()).mapToLong(Long::longValue).sum();
        long records = checks.stream().map(check -> ((Number) check.get("records")).longValue()).mapToLong(Long::longValue).sum();
        response.put("generatedAt", LocalDateTime.now());
        response.put("status", mismatches == 0 ? "PASS" : "REVIEW");
        response.put("score", records == 0 ? 100 : Math.max(0, 100 - Math.round(mismatches * 100.0 / records)));
        response.put("checks", checks);
        writeAudit("CRM_RECONCILIATION", "Checks " + checks.size() + " mismatches=" + mismatches);
        return response;
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private BigDecimal successfulPaymentAmount(List<Payment> payments) {
        return payments.stream()
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean countsAsRetainedBooking(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.COMPLETED;
    }

    private BigDecimal membershipRevenue(List<UserMembership> memberships) {
        return memberships.stream()
                .map(UserMembership::getPlan)
                .filter(Objects::nonNull)
                .map(plan -> plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isCompletedReferral(Referral referral) {
        return "COMPLETED".equalsIgnoreCase(safe(referral.getStatus()));
    }

    private boolean isActiveMembership(UserMembership membership) {
        return membership.getStatus() == MembershipStatus.ACTIVE
                && (membership.getEndsAt() == null || membership.getEndsAt().isAfter(LocalDateTime.now()));
    }

    private boolean expiresWithinSevenDays(UserMembership membership) {
        return isActiveMembership(membership)
                && membership.getEndsAt() != null
                && !membership.getEndsAt().isAfter(LocalDateTime.now().plusDays(7));
    }

    private long repeatBookerCount(List<Booking> bookings) {
        return bookings.stream()
                .filter(this::countsAsRetainedBooking)
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()))
                .values()
                .stream()
                .filter(count -> count > 1)
                .count();
    }

    private List<User> vipCustomers(List<User> users, List<Booking> bookings, List<Payment> payments) {
        Map<Long, Long> bookingCounts = bookings.stream()
                .filter(this::countsAsRetainedBooking)
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()));
        Map<Long, BigDecimal> spendByUser = payments.stream()
                .filter(payment -> "SUCCESS".equalsIgnoreCase(safe(payment.getStatus())))
                .collect(Collectors.groupingBy(payment -> payment.getBooking().getUser().getId(),
                        Collectors.mapping(payment -> payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount(), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        return users.stream()
                .filter(user -> isVip(bookingCounts.getOrDefault(user.getId(), 0L), spendByUser.getOrDefault(user.getId(), BigDecimal.ZERO)))
                .toList();
    }

    private List<User> inactiveCustomers(List<User> users, List<Booking> bookings, int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        Map<Long, Optional<LocalDate>> lastBookingByUser = bookings.stream()
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.mapping(Booking::getBookingDate, Collectors.maxBy(LocalDate::compareTo))));
        return users.stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> lastBookingByUser.getOrDefault(user.getId(), Optional.empty()).map(date -> date.isBefore(cutoff)).orElse(true))
                .toList();
    }

    private List<Map<String, Object>> buildSegments(List<User> users, List<Booking> bookings, List<Payment> payments, List<UserMembership> memberships) {
        Set<Long> activeMembers = memberships.stream().filter(this::isActiveMembership).map(membership -> membership.getUser().getId()).collect(Collectors.toSet());
        Set<Long> tournamentOrFrequentPlayers = bookings.stream()
                .filter(this::countsAsRetainedBooking)
                .collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= 5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<Long> weekendPlayers = bookings.stream()
                .filter(booking -> booking.getBookingDate() != null)
                .filter(booking -> booking.getBookingDate().getDayOfWeek().getValue() >= 6)
                .map(booking -> booking.getUser().getId())
                .collect(Collectors.toSet());
        return List.of(
                segment("VIP Customers", "Spend or booking-count based VIP segment", vipCustomers(users, bookings, payments).size(), "CLV >= 25000 or 10 retained bookings", "VIP_UPGRADE"),
                segment("Frequent Players", "Players with five or more retained bookings", tournamentOrFrequentPlayers.size(), "Retained bookings >= 5", "LOYALTY_BONUS"),
                segment("Weekend Players", "Players who book mostly during weekend demand windows", weekendPlayers.size(), "Any Saturday/Sunday booking", "WEEKEND_CAMPAIGN"),
                segment("Membership Customers", "Active paid membership users", activeMembers.size(), "Membership ACTIVE and not expired", "RENEWAL_CAMPAIGN"),
                segment("Inactive 30", "Active users without booking activity in 30 days", inactiveCustomers(users, bookings, 30).size(), "No booking in 30 days", "RE_ENGAGE_30"),
                segment("Inactive 90", "High-risk churn segment", inactiveCustomers(users, bookings, 90).size(), "No booking in 90 days", "WIN_BACK")
        );
    }

    private List<Map<String, Object>> playerOffers(int points, UserMembership activeMembership, List<Booking> bookings, List<Referral> referrals) {
        return List.of(
                offer("REDEEM_POINTS", "Convert loyalty points to wallet balance", points >= 10 ? "READY" : "LOCKED", points >= 10 ? "Redeem now" : nextMilestone(points) + " more point(s) to redeem"),
                offer("REFER_FRIEND", "Invite friends and earn referral rewards", referrals.stream().anyMatch(referral -> "PENDING".equalsIgnoreCase(referral.getStatus())) ? "PENDING" : "READY", "100 points after first booking"),
                offer("MEMBERSHIP_UPGRADE", "Unlock priority booking and discounts", activeMembership == null ? "READY" : "ACTIVE", activeMembership == null ? "Upgrade for member pricing" : "Benefits active"),
                offer("RE_ENGAGEMENT", "Return-to-play bonus", reEngagementStage(bookings), "Triggered by booking inactivity")
        );
    }

    private List<Map<String, Object>> growthInsights(long repeatBookers, long bookers, List<Referral> referrals, List<CouponUsage> couponUsages, List<UserMembership> memberships, List<MarketingCampaign> campaigns) {
        double repeatRate = percent(repeatBookers, Math.max(bookers, 1));
        double referralRate = percent(referrals.stream().filter(this::isCompletedReferral).count(), Math.max(referrals.size(), 1));
        List<Map<String, Object>> insights = new java.util.ArrayList<>();
        if (repeatRate < 25) {
            insights.add(insight("Repeat booking rate needs attention", "Launch loyalty or wallet-credit campaigns for one-time players.", "HIGH"));
        }
        if (!referrals.isEmpty() && referralRate < 40) {
            insights.add(insight("Referral conversion below target", "Review first-booking trigger and referral reward timing.", "MEDIUM"));
        }
        if (couponUsages.isEmpty()) {
            insights.add(insight("Coupon engine has low usage", "Promote active coupons through push and WhatsApp campaigns.", "MEDIUM"));
        }
        if (memberships.stream().noneMatch(this::isActiveMembership)) {
            insights.add(insight("Membership revenue not activated", "Publish Bronze, Silver, Gold, and Platinum value bundles.", "HIGH"));
        }
        if (campaigns.stream().noneMatch(campaign -> campaign.getStatus() == CampaignStatus.SCHEDULED || campaign.getStatus() == CampaignStatus.SENT)) {
            insights.add(insight("No active marketing automation", "Schedule welcome and re-engagement campaigns.", "MEDIUM"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("Growth engine stable", "Loyalty, referrals, coupons, memberships, and wallet controls are in balance.", "LOW"));
        }
        return insights;
    }

    private BigDecimal projectedClv(BigDecimal lifetimeSpend, List<Booking> bookings) {
        long activeMonths = Math.max(1, bookings.stream()
                .map(Booking::getBookingDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .map(firstDate -> ChronoUnit.MONTHS.between(firstDate.withDayOfMonth(1), LocalDate.now().withDayOfMonth(1)) + 1)
                .orElse(1L));
        BigDecimal monthlyRunRate = lifetimeSpend.divide(BigDecimal.valueOf(activeMonths), 2, RoundingMode.HALF_UP);
        return lifetimeSpend.add(monthlyRunRate.multiply(BigDecimal.valueOf(3))).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate lastBookingDate(List<Booking> bookings) {
        return bookings.stream().map(Booking::getBookingDate).filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
    }

    private String reEngagementStage(List<Booking> bookings) {
        LocalDate lastDate = lastBookingDate(bookings);
        if (lastDate == null) {
            return "WELCOME";
        }
        long days = ChronoUnit.DAYS.between(lastDate, LocalDate.now());
        if (days >= 90) return "WIN_BACK_90";
        if (days >= 60) return "RE_ENGAGE_60";
        if (days >= 30) return "RE_ENGAGE_30";
        return "ACTIVE";
    }

    private boolean isVip(long completedBookings, BigDecimal lifetimeSpend) {
        return completedBookings >= 10 || lifetimeSpend.compareTo(BigDecimal.valueOf(25000)) >= 0;
    }

    private String growthTier(long completedBookings, BigDecimal lifetimeSpend) {
        if (isVip(completedBookings, lifetimeSpend)) return "PLATINUM";
        if (completedBookings >= 5 || lifetimeSpend.compareTo(BigDecimal.valueOf(10000)) >= 0) return "GOLD";
        if (completedBookings >= 2 || lifetimeSpend.compareTo(BigDecimal.valueOf(3000)) >= 0) return "SILVER";
        return "BRONZE";
    }

    private BigDecimal pointsToWalletValue(int points) {
        return BigDecimal.valueOf(points).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
    }

    private int nextMilestone(int points) {
        int remainder = points % 100;
        return remainder == 0 ? 100 : 100 - remainder;
    }

    private String referralCode(User user) {
        return "CRIC-" + safe(user.getName()).toUpperCase(Locale.ROOT).replaceAll("\\s+", "") + "-" + user.getId();
    }

    private Map<String, Object> segment(String name, String description, long customers, String rule, String action) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("description", description);
        item.put("customers", customers);
        item.put("rule", rule);
        item.put("recommendedAction", action);
        return item;
    }

    private Map<String, Object> automation(String key, String trigger, long audience, String action) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("trigger", trigger);
        item.put("audience", audience);
        item.put("action", action);
        item.put("status", audience > 0 ? "READY" : "WATCH");
        return item;
    }

    private Map<String, Object> offer(String key, String title, String status, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("title", title);
        item.put("status", status);
        item.put("message", message);
        return item;
    }

    private Map<String, Object> insight(String title, String message, String severity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("message", message);
        item.put("severity", severity);
        return item;
    }

    private Map<String, Object> reconciliationCheck(String name, long records, long mismatches) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("records", records);
        item.put("mismatches", mismatches);
        item.put("status", mismatches == 0 ? "PASS" : "REVIEW");
        return item;
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(value * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void writeAudit(String action, String entity) {
        String admin = SecurityContextHolder.getContext().getAuthentication() == null
                ? "crm_growth_system"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(AuditLog.builder()
                .adminId(admin)
                .action(action)
                .entity(entity)
                .build());
    }
}
