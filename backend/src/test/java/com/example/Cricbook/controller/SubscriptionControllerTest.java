package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SubscriptionResponse;
import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.repository.MembershipPlanRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.RevenueExperimentService;
import com.example.Cricbook.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private MembershipPlanRepository planRepository;
    @Mock private RevenueExperimentService revenueExperimentService;
    @Mock private UserRepository userRepository;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @Test
    void currentOwnerSubscriptionIncludesExpiryForSummaryTile() {
        LocalDateTime expiry = LocalDateTime.of(2026, 11, 11, 22, 7);
        MembershipPlan plan = MembershipPlan.builder()
                .id(4L)
                .name("ENTERPRISE")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(new BigDecimal("4999.00"))
                .durationDays(30)
                .build();
        Subscription subscription = Subscription.builder()
                .id(15L)
                .plan(plan)
                .status(MembershipStatus.ACTIVE)
                .startDate(LocalDateTime.of(2026, 10, 11, 22, 7))
                .endDate(expiry)
                .autoRenew(true)
                .build();
        when(userDetails.getUsername()).thenReturn("owner@example.com");
        when(subscriptionService.getCurrentSubscription("owner@example.com", "OWNER_FEATURED"))
                .thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response =
                subscriptionController.getCurrentSubscription(userDetails, "OWNER_FEATURED");

        assertNotNull(response.getBody());
        assertEquals("ENTERPRISE", response.getBody().getPlanName());
        assertEquals(expiry, response.getBody().getEndDate());
        assertEquals(true, response.getBody().getAutoRenew());
    }

    @Test
    void ownerCanDisableAutoRenewAndReceiveUpdatedSubscriptionState() {
        MembershipPlan plan = MembershipPlan.builder()
                .id(4L)
                .name("ENTERPRISE")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(new BigDecimal("4999.00"))
                .durationDays(30)
                .build();
        Subscription subscription = Subscription.builder()
                .id(15L)
                .plan(plan)
                .status(MembershipStatus.ACTIVE)
                .startDate(LocalDateTime.of(2026, 10, 11, 22, 7))
                .endDate(LocalDateTime.of(2026, 11, 11, 22, 7))
                .autoRenew(false)
                .build();
        when(userDetails.getUsername()).thenReturn("owner@example.com");
        when(subscriptionService.setAutoRenew("owner@example.com", "OWNER_FEATURED", false))
                .thenReturn(subscription);

        ResponseEntity<SubscriptionResponse> response =
                subscriptionController.setAutoRenew(userDetails, false, "OWNER_FEATURED");

        assertNotNull(response.getBody());
        assertEquals("ENTERPRISE", response.getBody().getPlanName());
        assertEquals(false, response.getBody().getAutoRenew());
    }

    @Test
    void ownerPricingIncludesFieldsRequiredByPlanComparisonCards() {
        User owner = User.builder().id(8L).email("owner@example.com").build();
        MembershipPlan plan = MembershipPlan.builder()
                .id(4L)
                .name("ENTERPRISE")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(new BigDecimal("4999.00"))
                .durationDays(30)
                .discountPercent(0)
                .prioritySlots(true)
                .featuredListing(true)
                .description("Advanced reports and unlimited turf listings")
                .build();
        when(userDetails.getUsername()).thenReturn("owner@example.com");
        when(userRepository.findByEmailOrPhone("owner@example.com")).thenReturn(Optional.of(owner));
        when(revenueExperimentService.getAssignedVariant(8L)).thenReturn("A");
        when(planRepository.findByTypeOrderByPriceAsc(MembershipPlanType.OWNER_FEATURED)).thenReturn(List.of(plan));

        ResponseEntity<Map<String, Object>> response =
                subscriptionController.getPricingPage(userDetails, "OWNER_FEATURED");

        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> plans = (List<Map<String, Object>>) response.getBody().get("plans");
        assertEquals(1, plans.size());
        assertEquals("ENTERPRISE", plans.getFirst().get("name"));
        assertEquals(new BigDecimal("4999.00"), plans.getFirst().get("price"));
        assertEquals(30, plans.getFirst().get("durationDays"));
        assertEquals(true, plans.getFirst().get("featuredListing"));
    }
}
