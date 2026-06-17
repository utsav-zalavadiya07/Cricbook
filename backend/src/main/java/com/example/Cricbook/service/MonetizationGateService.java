package com.example.Cricbook.service;

import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.entity.UsageTracking;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.FeatureAccessRepository;
import com.example.Cricbook.repository.SubscriptionRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UsageTrackingRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonetizationGateService {

    private final SubscriptionRepository subscriptionRepository;
    private final FeatureAccessRepository featureAccessRepository;
    private final UsageTrackingRepository usageTrackingRepository;
    private final UserRepository userRepository;
    private final TurfRepository turfRepository;

    @Transactional(readOnly = true)
    public String getActivePlanName(Long userId) {
        return getActivePlanName(userId, null);
    }

    private String getActivePlanName(Long userId, String feature) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        MembershipPlanType planType = resolvePlanType(feature, user);
        Optional<Subscription> activeSub = subscriptionRepository.findFirstDetailedActiveSubscriptionByType(userId, LocalDateTime.now(), planType);
        if (activeSub.isPresent()) {
            return activeSub.get().getPlan().getName().toUpperCase();
        }

        return planType == MembershipPlanType.OWNER_FEATURED ? "STARTER" : "FREE";
    }

    @Transactional(readOnly = true)
    public boolean isFeatureAllowed(Long userId, String feature) {
        String planName = getActivePlanName(userId, feature);
        
        // Elite & Enterprise bypass features checks (unlimited premium benefits)
        if ("ELITE".equals(planName) || "ENTERPRISE".equals(planName)) {
            return true;
        }
        
        // Basic features allowed for PRO and BUSINESS
        if ("PRO".equals(planName) && ("PRIORITY_SLOTS".equalsIgnoreCase(feature) || "FAVORITES".equalsIgnoreCase(feature))) {
            return true;
        }

        if ("BUSINESS".equals(planName) && ("ANALYTICS".equalsIgnoreCase(feature) || "PROMOTIONS".equalsIgnoreCase(feature))) {
            return true;
        }

        return featureAccessRepository.existsByPlanNameAndFeature(planName, feature);
    }

    @Transactional
    public void trackAndCheckUsage(Long userId, String feature) {
        String planName = getActivePlanName(userId, feature);
        int limit = getLimitForPlan(planName, feature);

        if ("TURFS".equalsIgnoreCase(feature)) {
            long currentTurfs = turfRepository.countByOwnerId(userId);
            if (currentTurfs >= limit) {
                throw new ApiException("Limit reached for " + feature + " under your current " + planName + " plan (" + limit + " allowed). Please upgrade your owner subscription.");
            }
            return;
        }

        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        UsageTracking tracking = usageTrackingRepository
                .findByUserIdAndFeatureAndPeriodMonth(userId, feature, currentMonth)
                .orElseGet(() -> UsageTracking.builder()
                        .userId(userId)
                        .feature(feature)
                        .usageCount(0)
                        .periodMonth(currentMonth)
                        .build());

        if (tracking.getUsageCount() >= limit) {
            throw new ApiException("Limit reached for " + feature + " under your current " + planName + " plan (" + limit + " allowed). Please upgrade your subscription.");
        }

        tracking.setUsageCount(tracking.getUsageCount() + 1);
        usageTrackingRepository.save(tracking);
    }

    private MembershipPlanType resolvePlanType(String feature, User user) {
        if (isOwnerFeature(feature)) {
            return MembershipPlanType.OWNER_FEATURED;
        }
        if (isPlayerFeature(feature)) {
            return MembershipPlanType.USER_PREMIUM;
        }
        return isOwner(user) ? MembershipPlanType.OWNER_FEATURED : MembershipPlanType.USER_PREMIUM;
    }

    private boolean isOwnerFeature(String feature) {
        String normalized = normalizeFeature(feature);
        return "TURFS".equals(normalized) || "PROMOTIONS".equals(normalized) || "ANALYTICS".equals(normalized);
    }

    private boolean isPlayerFeature(String feature) {
        String normalized = normalizeFeature(feature);
        return "BOOKINGS".equals(normalized)
                || "PRIORITY_SLOTS".equals(normalized)
                || "FAVORITES".equals(normalized)
                || "EXCLUSIVE_EVENTS".equals(normalized);
    }

    private String normalizeFeature(String feature) {
        return feature == null ? "" : feature.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isOwner(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("OWNER") || r.getName().name().equals("ROLE_OWNER"));
    }

    private int getLimitForPlan(String planName, String feature) {
        if ("BOOKINGS".equalsIgnoreCase(feature)) {
            if ("FREE".equals(planName)) return 3;
            if ("PRO".equals(planName)) return 15;
            return 999999; // ELITE is effectively unlimited
        }
        if ("TURFS".equalsIgnoreCase(feature)) {
            if ("STARTER".equals(planName)) return 1;
            if ("BUSINESS".equals(planName)) return 5;
            return 999999; // ENTERPRISE is unlimited
        }
        if ("PROMOTIONS".equalsIgnoreCase(feature)) {
            if ("STARTER".equals(planName)) return 0;
            if ("BUSINESS".equals(planName)) return 3;
            return 999999; // ENTERPRISE is unlimited
        }
        return 999999;
    }
}
