package com.example.Cricbook.service;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MembershipPlanRepository planRepository;
    private final UserRepository userRepository;
    private final TurfRepository turfRepository;
    private final WalletService walletService;
    private final InvoiceRepository invoiceRepository;
    private final TaxRecordRepository taxRecordRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final FeatureAccessRepository featureAccessRepository;
    private final WalletRepository walletRepository;
    private final RevenueExperimentService revenueExperimentService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional
    public void ensureDefaultPlans() {
        ensurePlan("FREE", MembershipPlanType.USER_PREMIUM, BigDecimal.ZERO, 36500, 0, false, false,
                "Basic player plan. Max 3 bookings per month.");
        ensurePlan("PRO", MembershipPlanType.USER_PREMIUM, BigDecimal.valueOf(299), 30, 10, true, false,
                "Priority slot bookings and 10% discount. Max 15 bookings per month.");
        ensurePlan("ELITE", MembershipPlanType.USER_PREMIUM, BigDecimal.valueOf(599), 30, 20, true, false,
                "Exclusive event access, priority support and 20% discount. Unlimited bookings.");

        ensurePlan("STARTER", MembershipPlanType.OWNER_FEATURED, BigDecimal.ZERO, 36500, 0, false, false,
                "Basic turf owner plan. Max 1 turf, no campaigns.");
        ensurePlan("BUSINESS", MembershipPlanType.OWNER_FEATURED, BigDecimal.valueOf(1499), 30, 0, false, true,
                "Featured listings, promo campaign manager, and basic business report. Max 5 turfs.");
        ensurePlan("ENTERPRISE", MembershipPlanType.OWNER_FEATURED, BigDecimal.valueOf(4999), 30, 0, false, true,
                "Unlimited turf listings, full campaign promotions, detailed metrics, and API access.");

        ensureFeature("PRO", "PRIORITY_SLOTS");
        ensureFeature("PRO", "FAVORITES");
        ensureFeature("ELITE", "PRIORITY_SLOTS");
        ensureFeature("ELITE", "FAVORITES");
        ensureFeature("ELITE", "EXCLUSIVE_EVENTS");
        ensureFeature("ELITE", "PRIORITY_SUPPORT");
        ensureFeature("BUSINESS", "ANALYTICS");
        ensureFeature("BUSINESS", "PROMOTIONS");
        ensureFeature("ENTERPRISE", "ANALYTICS");
        ensureFeature("ENTERPRISE", "PROMOTIONS");
        ensureFeature("ENTERPRISE", "UNLIMITED_TURFS");
        ensureFeature("ENTERPRISE", "API_ACCESS");
    }

    private MembershipPlan ensurePlan(
            String name,
            MembershipPlanType type,
            BigDecimal price,
            int durationDays,
            int discountPercent,
            boolean prioritySlots,
            boolean featuredListing,
            String description) {
        List<MembershipPlan> existingPlans = Optional.ofNullable(planRepository.findByTypeOrderByPriceAsc(type)).orElse(List.of());
        return existingPlans.stream()
                .filter(plan -> name.equalsIgnoreCase(plan.getName()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Creating missing {} subscription plan {}", type, name);
                    return planRepository.save(MembershipPlan.builder()
                            .name(name)
                            .type(type)
                            .price(price)
                            .durationDays(durationDays)
                            .discountPercent(discountPercent)
                            .prioritySlots(prioritySlots)
                            .featuredListing(featuredListing)
                            .description(description)
                            .build());
                });
    }

    private void ensureFeature(String planName, String feature) {
        if (!featureAccessRepository.existsByPlanNameAndFeature(planName, feature)) {
            featureAccessRepository.save(FeatureAccess.builder().planName(planName).feature(feature).build());
        }
    }

    @Transactional
    public Subscription startSubscription(String username, Long planId, Long turfId) {
        ensureDefaultPlans();

        User user = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("User not found"));

        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ApiException("Membership plan not found"));

        if (plan.getType() == MembershipPlanType.OWNER_FEATURED
                && Boolean.TRUE.equals(plan.getFeaturedListing())
                && turfId == null) {
            throw new ApiException("Select a turf to receive the featured listing benefit");
        }

        Turf turf = null;
        if (plan.getType() == MembershipPlanType.OWNER_FEATURED && turfId != null) {
            turf = turfRepository.findById(turfId)
                    .orElseThrow(() -> new ApiException("Turf not found"));
            if (!turf.getOwner().getId().equals(user.getId())) {
                throw new ApiException("You can promote only your own turf");
            }
        }

        // Validate single active plan and tier upgrade rules before deducting money
        List<Subscription> activeSubs = subscriptionRepository.findDetailedActiveSubscriptionsByType(
                user.getId(), MembershipStatus.ACTIVE, LocalDateTime.now(), plan.getType());

        Subscription activePaidSub = activeSubs.stream()
                .filter(s -> s.getPlan() != null && s.getPlan().getPrice().compareTo(BigDecimal.ZERO) > 0)
                .max(java.util.Comparator.comparing(s -> s.getPlan().getPrice()))
                .orElse(null);

        if (activePaidSub != null) {
            MembershipPlan currentPlan = activePaidSub.getPlan();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formattedExpiry = activePaidSub.getEndDate() != null
                    ? activePaidSub.getEndDate().format(formatter)
                    : "the end of your billing cycle";

            // Rule 1: Cannot repurchase the same plan while it is active
            if (currentPlan.getId().equals(plan.getId()) || currentPlan.getName().equalsIgnoreCase(plan.getName())) {
                throw new ApiException("You already have an active " + currentPlan.getName() + " plan valid until " + formattedExpiry + ". You cannot repurchase the same plan while it is active.");
            }

            // Rule 2: Cannot downgrade to a lower plan while current plan is active
            if (plan.getPrice().compareTo(currentPlan.getPrice()) <= 0) {
                throw new ApiException("You cannot switch to a lower plan (" + plan.getName() + ") while your " + currentPlan.getName() + " plan is active until " + formattedExpiry + ". You can switch to another plan once your current plan expires.");
            }

            log.info("User {} is upgrading subscription from {} to {}", user.getEmail(), currentPlan.getName(), plan.getName());
        }

        // Apply A/B price experiment (15% discount for PRO plan under Variant B)
        BigDecimal price = plan.getPrice();
        String variant = revenueExperimentService.getAssignedVariant(user.getId());
        if ("PRO".equalsIgnoreCase(plan.getName()) && "B".equalsIgnoreCase(variant)) {
            price = price.multiply(BigDecimal.valueOf(0.85)).setScale(2, RoundingMode.HALF_UP);
            log.info("A/B Pricing Test: Applied 15% Variant B discount to plan PRO. Final price: Rs {}", price);
        }

        if (price.compareTo(BigDecimal.ZERO) > 0) {
            revenueExperimentService.trackConversion(variant);
            Wallet wallet = walletRepository.findByUserId(user.getId())
                    .orElseGet(() -> walletRepository.save(Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .build()));

            // Credit Card simulation: top up wallet balance if insufficient
            if (wallet.getBalance().compareTo(price) < 0) {
                BigDecimal deficit = price.subtract(wallet.getBalance());
                log.info("Simulating payment gateway credit card charge for Rs {}...", deficit);
                
                wallet.setBalance(wallet.getBalance().add(deficit));
                walletRepository.save(wallet);
                
                // Add topup audit log
                walletService.addMoney(username, new com.example.Cricbook.dto.AddMoneyRequest(deficit));
            }

            walletService.debitWallet(user, price, "SUBSCRIPTION_CHARGE", "Purchased " + plan.getName() + " membership");
        }

        // Supersede any previous active subscriptions of the same type
        for (Subscription sub : activeSubs) {
            sub.setStatus(MembershipStatus.CANCELLED);
            subscriptionRepository.save(sub);
        }

        // Calculate exact start and expiration dates (matching exact day and time)
        LocalDateTime startDate = LocalDateTime.now().withNano(0);
        LocalDateTime endDate;
        if (plan.getDurationDays() > 0 && plan.getDurationDays() % 30 == 0 && plan.getDurationDays() <= 365) {
            endDate = startDate.plusMonths(plan.getDurationDays() / 30);
        } else {
            endDate = startDate.plusDays(plan.getDurationDays());
        }

        Subscription subscription = subscriptionRepository.save(Subscription.builder()
                .user(user)
                .plan(plan)
                .turf(turf)
                .status(MembershipStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(true)
                .build());

        // Update Turf Featured status if applicable
        if (turf != null && Boolean.TRUE.equals(plan.getFeaturedListing())) {
            turf.setFeaturedUntil(endDate);
            turfRepository.save(turf);
        }

        InclusiveTaxCalculator.TaxBreakdown taxBreakdown = InclusiveTaxCalculator.fromInclusiveTotal(price);

        try {
            jdbcTemplate.execute("ALTER TABLE invoices MODIFY COLUMN booking_id bigint DEFAULT NULL");
        } catch (Exception e) {
            log.warn("Could not alter invoices table", e);
        }

        // Generate Invoice
        String invoiceNum = "INV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .invoiceNumber(invoiceNum)
                .user(user)
                .subscription(subscription)
                .amount(taxBreakdown.taxableAmount())
                .taxAmount(taxBreakdown.taxAmount())
                .status("PAID")
                .invoiceUrl(invoiceUrl(invoiceNum))
                .build());

        // Create Tax Record
        taxRecordRepository.save(TaxRecord.builder()
                .invoice(invoice)
                .gstAmount(taxBreakdown.taxAmount())
                .taxRate(InclusiveTaxCalculator.GST_RATE_FRACTION)
                .build());

        // Create Revenue record
        revenueRecordRepository.save(RevenueRecord.builder()
                .source("SUBSCRIPTION")
                .amount(price)
                .build());

        return subscription;
    }

    @Transactional
    public Subscription getCurrentSubscription(String username) {
        return getCurrentSubscription(username, null);
    }

    @Transactional
    public Subscription getCurrentSubscription(String username, String type) {
        ensureDefaultPlans();

        User user = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("User not found"));

        MembershipPlanType planType = resolvePlanType(type, user);
        Optional<Subscription> subscription = subscriptionRepository.findFirstDetailedActiveSubscriptionByType(user.getId(), LocalDateTime.now(), planType);
        if (subscription.isPresent()) {
            return subscription.get();
        }

        String defaultPlanName = defaultPlanName(planType);
        MembershipPlan defaultPlan = planRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(defaultPlanName))
                .filter(p -> p.getType() == planType)
                .findFirst()
                .orElseGet(() -> planRepository.save(MembershipPlan.builder()
                        .name(defaultPlanName)
                        .type(planType)
                        .price(BigDecimal.ZERO)
                        .durationDays(36500)
                        .discountPercent(0)
                        .prioritySlots(false)
                        .featuredListing(false)
                        .description("Default basic plan")
                        .build()));

        return Subscription.builder()
                .user(user)
                .plan(defaultPlan)
                .status(MembershipStatus.ACTIVE)
                .startDate(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now())
                .endDate(LocalDateTime.now().plusYears(10))
                .autoRenew(false)
                .build();
    }

    @Transactional
    public Subscription cancelSubscription(String username) {
        return cancelSubscription(username, null);
    }

    @Transactional
    public Subscription cancelSubscription(String username, String type) {
        return setAutoRenew(username, type, false);
    }

    @Transactional
    public Subscription setAutoRenew(String username, String type, boolean enabled) {
        User user = userRepository.findByEmailOrPhone(username)
                .orElseThrow(() -> new ApiException("User not found"));

        MembershipPlanType planType = resolvePlanType(type, user);
        Subscription activeSub = subscriptionRepository.findFirstDetailedActiveSubscriptionByType(user.getId(), LocalDateTime.now(), planType)
                .orElseThrow(() -> new ApiException("No active paid subscription found"));

        activeSub.setAutoRenew(enabled);
        return subscriptionRepository.save(activeSub);
    }

    private MembershipPlanType resolvePlanType(String type, User user) {
        if (type != null && !type.isBlank()) {
            try {
                return MembershipPlanType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ApiException("Unsupported subscription type.");
            }
        }

        return isOwner(user) ? MembershipPlanType.OWNER_FEATURED : MembershipPlanType.USER_PREMIUM;
    }

    private boolean isOwner(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() != null
                        && ("OWNER".equals(r.getName().name()) || "ROLE_OWNER".equals(r.getName().name())));
    }

    private String defaultPlanName(MembershipPlanType type) {
        return type == MembershipPlanType.OWNER_FEATURED ? "STARTER" : "FREE";
    }

    private String invoiceUrl(String invoiceNumber) {
        return "/api/v1/billing/invoices/number/" + invoiceNumber + "/download";
    }
}
