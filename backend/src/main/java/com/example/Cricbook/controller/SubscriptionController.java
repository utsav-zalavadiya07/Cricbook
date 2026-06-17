package com.example.Cricbook.controller;

import com.example.Cricbook.dto.StartSubscriptionRequest;
import com.example.Cricbook.dto.SubscriptionResponse;
import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final com.example.Cricbook.repository.MembershipPlanRepository planRepository;
    private final com.example.Cricbook.service.RevenueExperimentService revenueExperimentService;
    private final com.example.Cricbook.repository.UserRepository userRepository;

    @GetMapping("/pricing")
    public ResponseEntity<java.util.Map<String, Object>> getPricingPage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {
        subscriptionService.ensureDefaultPlans();
        
        com.example.Cricbook.entity.User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> new com.example.Cricbook.exception.ApiException("User not found"));

        String variant = revenueExperimentService.getAssignedVariant(user.getId());
        revenueExperimentService.trackImpression(variant);

        MembershipPlanType requestedType = parsePlanType(type);
        java.util.List<com.example.Cricbook.entity.MembershipPlan> plans = requestedType == null
                ? planRepository.findAllByOrderByPriceAsc()
                : planRepository.findByTypeOrderByPriceAsc(requestedType);
        java.util.List<java.util.Map<String, Object>> plansResponse = plans.stream().map(p -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("type", p.getType().name());
            map.put("durationDays", p.getDurationDays());
            map.put("discountPercent", p.getDiscountPercent());
            map.put("prioritySlots", p.getPrioritySlots());
            map.put("featuredListing", p.getFeaturedListing());
            map.put("description", p.getDescription());
            
            java.math.BigDecimal finalPrice = p.getPrice();
            if ("PRO".equalsIgnoreCase(p.getName()) && "B".equalsIgnoreCase(variant)) {
                finalPrice = finalPrice.multiply(java.math.BigDecimal.valueOf(0.85)).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            map.put("price", finalPrice);
            map.put("originalPrice", p.getPrice());
            return map;
        }).toList();

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("plans", plansResponse);
        response.put("variant", variant);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/start")
    public ResponseEntity<SubscriptionResponse> startSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StartSubscriptionRequest request) {
        Subscription sub = subscriptionService.startSubscription(
                userDetails.getUsername(),
                request.getPlanId(),
                request.getTurfId()
        );
        return ResponseEntity.ok(mapSubscription(sub));
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {
        Subscription sub = subscriptionService.getCurrentSubscription(userDetails.getUsername(), type);
        return ResponseEntity.ok(mapSubscription(sub));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {
        Subscription sub = subscriptionService.cancelSubscription(userDetails.getUsername(), type);
        return ResponseEntity.ok(mapSubscription(sub));
    }

    private SubscriptionResponse mapSubscription(Subscription sub) {
        var plan = sub.getPlan();
        var turf = sub.getTurf();

        return SubscriptionResponse.builder()
                .id(sub.getId())
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null && plan.getName() != null ? plan.getName() : "FREE")
                .type(plan != null && plan.getType() != null ? plan.getType().name() : "USER_PREMIUM")
                .price(plan != null ? plan.getPrice() : java.math.BigDecimal.ZERO)
                .durationDays(plan != null ? plan.getDurationDays() : 0)
                .status(sub.getStatus() != null ? sub.getStatus().name() : "ACTIVE")
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .autoRenew(Boolean.TRUE.equals(sub.getAutoRenew()))
                .turfId(turf != null ? turf.getId() : null)
                .turfName(turf != null ? turf.getName() : null)
                .build();
    }

    private MembershipPlanType parsePlanType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        try {
            return MembershipPlanType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Unsupported subscription type.");
        }
    }
}
