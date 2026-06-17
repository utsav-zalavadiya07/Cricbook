package com.example.Cricbook.service;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.FeatureAccessRepository;
import com.example.Cricbook.repository.SubscriptionRepository;
import com.example.Cricbook.repository.TurfRepository;
import com.example.Cricbook.repository.UsageTrackingRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonetizationGateServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private FeatureAccessRepository featureAccessRepository;
    @Mock private UsageTrackingRepository usageTrackingRepository;
    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;

    @InjectMocks
    private MonetizationGateService monetizationGateService;

    @Test
    void isFeatureAllowedEliteHasBypass() {
        User user = User.builder().id(1L).email("elite@test.com").roles(Collections.emptySet()).build();
        MembershipPlan plan = MembershipPlan.builder().name("ELITE").type(MembershipPlanType.USER_PREMIUM).build();
        Subscription activeSub = Subscription.builder().plan(plan).status(MembershipStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(LocalDateTime.class), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(Optional.of(activeSub));

        boolean allowed = monetizationGateService.isFeatureAllowed(1L, "EXCLUSIVE_EVENTS");
        assertTrue(allowed);
    }

    @Test
    void trackAndCheckUsageThrowsExceptionOnLimitReached() {
        User user = User.builder().id(1L).email("free@test.com").roles(Collections.emptySet()).build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // No active subscription -> defaults to FREE
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(LocalDateTime.class), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(Optional.empty());

        UsageTracking fullTracking = UsageTracking.builder()
                .userId(1L)
                .feature("BOOKINGS")
                .usageCount(3) // FREE limit is 3 bookings per month
                .periodMonth("2026-05")
                .build();

        when(usageTrackingRepository.findByUserIdAndFeatureAndPeriodMonth(eq(1L), eq("BOOKINGS"), anyString()))
                .thenReturn(Optional.of(fullTracking));

        assertThrows(ApiException.class, () -> monetizationGateService.trackAndCheckUsage(1L, "BOOKINGS"));
    }

    @Test
    void trackAndCheckUsageForTurfsUsesOwnerPlanAndCurrentTurfCount() {
        User owner = User.builder().id(1L).email("owner@test.com").roles(Collections.singleton(new Role(1L, RoleName.ROLE_OWNER))).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(LocalDateTime.class), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Optional.empty());
        when(turfRepository.countByOwnerId(1L)).thenReturn(1L);

        ApiException exception = assertThrows(ApiException.class, () -> monetizationGateService.trackAndCheckUsage(1L, "TURFS"));

        assertTrue(exception.getMessage().contains("STARTER"));
        assertTrue(exception.getMessage().contains("owner subscription"));
        verify(usageTrackingRepository, never()).save(any());
    }

    @Test
    void trackAndCheckUsageForTurfsAllowsBusinessOwnerBeforeLimit() {
        User owner = User.builder().id(1L).email("owner@test.com").roles(Collections.singleton(new Role(1L, RoleName.ROLE_OWNER))).build();
        MembershipPlan plan = MembershipPlan.builder().name("BUSINESS").type(MembershipPlanType.OWNER_FEATURED).build();
        Subscription activeSub = Subscription.builder().plan(plan).status(MembershipStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(LocalDateTime.class), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Optional.of(activeSub));
        when(turfRepository.countByOwnerId(1L)).thenReturn(4L);

        assertDoesNotThrow(() -> monetizationGateService.trackAndCheckUsage(1L, "TURFS"));

        verify(usageTrackingRepository, never()).save(any());
    }
}
