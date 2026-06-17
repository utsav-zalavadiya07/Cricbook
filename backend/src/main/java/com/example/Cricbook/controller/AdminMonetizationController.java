package com.example.Cricbook.controller;

import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.entity.RevenueRecord;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.MembershipPlanRepository;
import com.example.Cricbook.repository.RevenueRecordRepository;
import com.example.Cricbook.repository.SubscriptionRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMonetizationController {

    private final SubscriptionRepository subscriptionRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final MembershipPlanRepository planRepository;
    private final UserRepository userRepository;

    @GetMapping("/revenue")
    public ResponseEntity<Map<String, Object>> getFinancialAnalytics() {
        // Calculate MRR
        BigDecimal mrr = subscriptionRepository.findAll().stream()
                .filter(sub -> "ACTIVE".equals(sub.getStatus().name()))
                .map(sub -> sub.getPlan().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate ARR
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12));

        // Calculate Total Revenue
        List<RevenueRecord> records = revenueRecordRepository.findAll();
        BigDecimal totalRevenue = records.stream()
                .map(RevenueRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate LTV (Average Revenue Per User)
        long totalUsers = userRepository.count();
        BigDecimal ltv = totalUsers > 0 
                ? totalRevenue.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        // Calculate Churn Rate (cancelled subscriptions / total subscriptions)
        long totalSubs = subscriptionRepository.count();
        long cancelledSubs = subscriptionRepository.findAll().stream()
                .filter(sub -> "CANCELLED".equals(sub.getStatus().name()) || "EXPIRED".equals(sub.getStatus().name()))
                .count();
        double churnRate = totalSubs > 0 ? ((double) cancelledSubs / totalSubs) * 100 : 0.0;

        // Calculate Conversion Rate (Upgraded / Total Players)
        long totalPlayers = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "PLAYER".equals(r.getName().name()) || "ROLE_PLAYER".equals(r.getName().name())))
                .count();
        long upgradedPlayers = subscriptionRepository.findAll().stream()
                .filter(sub -> "ACTIVE".equals(sub.getStatus().name()) && sub.getPlan().getType() == MembershipPlanType.USER_PREMIUM && sub.getPlan().getPrice().compareTo(BigDecimal.ZERO) > 0)
                .map(sub -> sub.getUser().getId())
                .distinct()
                .count();
        double conversionRate = totalPlayers > 0 ? ((double) upgradedPlayers / totalPlayers) * 100 : 0.0;

        // Generate monthly series (simulated/aggregated breakdown from revenue records)
        BigDecimal subscriptionRevenue = records.stream()
                .filter(r -> "SUBSCRIPTION".equalsIgnoreCase(r.getSource()))
                .map(RevenueRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bookingRevenue = records.stream()
                .filter(r -> "BOOKING".equalsIgnoreCase(r.getSource()))
                .map(RevenueRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal promotionRevenue = records.stream()
                .filter(r -> "PROMOTION".equalsIgnoreCase(r.getSource()))
                .map(RevenueRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("mrr", mrr);
        response.put("arr", arr);
        response.put("ltv", ltv);
        response.put("totalRevenue", totalRevenue);
        response.put("churnRate", Math.round(churnRate * 10.0) / 10.0);
        response.put("conversionRate", Math.round(conversionRate * 10.0) / 10.0);
        response.put("subscriptionRevenue", subscriptionRevenue);
        response.put("bookingRevenue", bookingRevenue);
        response.put("promotionRevenue", promotionRevenue);
        response.put("totalUsers", totalUsers);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // CRUD MEMBERSHIP PLANS
    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlan>> listPlans() {
        return ResponseEntity.ok(planRepository.findAll());
    }

    @PostMapping("/plans")
    public ResponseEntity<MembershipPlan> createPlan(@RequestBody MembershipPlan plan) {
        if (plan.getName() == null || plan.getName().isBlank()) {
            throw new ApiException("Plan name is required");
        }
        return ResponseEntity.ok(planRepository.save(plan));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<MembershipPlan> updatePlan(@PathVariable Long id, @RequestBody MembershipPlan planDetails) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ApiException("Plan not found"));
        plan.setName(planDetails.getName());
        plan.setPrice(planDetails.getPrice());
        plan.setDurationDays(planDetails.getDurationDays());
        plan.setDescription(planDetails.getDescription());
        plan.setDiscountPercent(planDetails.getDiscountPercent());
        plan.setPrioritySlots(planDetails.getPrioritySlots());
        plan.setFeaturedListing(planDetails.getFeaturedListing());
        return ResponseEntity.ok(planRepository.save(plan));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Map<String, String>> deletePlan(@PathVariable Long id) {
        MembershipPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ApiException("Plan not found"));
        planRepository.delete(plan);
        return ResponseEntity.ok(Map.of("message", "Plan deleted successfully"));
    }
}
