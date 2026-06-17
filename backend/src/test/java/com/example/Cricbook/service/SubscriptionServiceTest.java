package com.example.Cricbook.service;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MembershipPlanRepository planRepository;
    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private WalletService walletService;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TaxRecordRepository taxRecordRepository;
    @Mock private RevenueRecordRepository revenueRecordRepository;
    @Mock private FeatureAccessRepository featureAccessRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private RevenueExperimentService revenueExperimentService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void startSubscriptionCreatesPaidSubscription() {
        User user = User.builder().id(1L).email("user@test.com").roles(Collections.emptySet()).build();
        MembershipPlan plan = MembershipPlan.builder()
                .id(2L)
                .name("PRO")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .discountPercent(10)
                .prioritySlots(true)
                .featuredListing(false)
                .build();

        Wallet wallet = Wallet.builder().user(user).balance(BigDecimal.valueOf(500)).build();

        when(userRepository.findByEmailOrPhone("user@test.com")).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(revenueExperimentService.getAssignedVariant(1L)).thenReturn("A");
        when(subscriptionRepository.findActiveSubscriptions(eq(1L), eq(MembershipStatus.ACTIVE), any())).thenReturn(Collections.emptyList());

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });

        Subscription result = subscriptionService.startSubscription("user@test.com", 2L, null);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(MembershipStatus.ACTIVE, result.getStatus());
        verify(walletService, times(1)).debitWallet(eq(user), eq(BigDecimal.valueOf(299)), eq("SUBSCRIPTION_CHARGE"), anyString());
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, times(1)).save(invoiceCaptor.capture());
        assertNotNull(invoiceCaptor.getValue().getInvoiceUrl());
        assertTrue(invoiceCaptor.getValue().getInvoiceUrl().startsWith("/api/v1/billing/invoices/number/INV-"));
        verify(taxRecordRepository, times(1)).save(any(TaxRecord.class));
        verify(revenueRecordRepository, times(1)).save(any(RevenueRecord.class));
    }

    @Test
    void getCurrentSubscriptionByTypeReturnsMatchingOwnerPlan() {
        User user = User.builder().id(1L).email("owner@test.com").roles(Collections.emptySet()).build();
        MembershipPlan ownerPlan = MembershipPlan.builder()
                .id(10L)
                .name("BUSINESS")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.valueOf(1499))
                .durationDays(30)
                .build();
        Subscription ownerSubscription = Subscription.builder()
                .id(20L)
                .user(user)
                .plan(ownerPlan)
                .status(MembershipStatus.ACTIVE)
                .autoRenew(true)
                .build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Optional.of(ownerSubscription));

        Subscription result = subscriptionService.getCurrentSubscription("owner@test.com", "OWNER_FEATURED");

        assertEquals(20L, result.getId());
        assertEquals("BUSINESS", result.getPlan().getName());
        assertEquals(MembershipPlanType.OWNER_FEATURED, result.getPlan().getType());
    }

    @Test
    void getCurrentSubscriptionUsesOwnerDefaultWhenNoTypeProvidedForOwner() {
        User user = User.builder()
                .id(1L)
                .email("owner@test.com")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_OWNER).build()))
                .build();
        MembershipPlan starterPlan = MembershipPlan.builder()
                .id(11L)
                .name("STARTER")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.ZERO)
                .durationDays(36500)
                .build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Optional.empty());
        when(planRepository.findAll()).thenReturn(List.of(starterPlan));

        Subscription result = subscriptionService.getCurrentSubscription("owner@test.com");

        assertEquals("STARTER", result.getPlan().getName());
        assertEquals(MembershipPlanType.OWNER_FEATURED, result.getPlan().getType());
        assertEquals(MembershipStatus.ACTIVE, result.getStatus());
    }

    @Test
    void cancelSubscriptionByTypeCancelsOnlyMatchingPlanType() {
        User user = User.builder().id(1L).email("owner@test.com").roles(Collections.emptySet()).build();
        MembershipPlan ownerPlan = MembershipPlan.builder()
                .id(12L)
                .name("BUSINESS")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.valueOf(1499))
                .durationDays(30)
                .build();
        Subscription ownerSubscription = Subscription.builder()
                .id(30L)
                .user(user)
                .plan(ownerPlan)
                .status(MembershipStatus.ACTIVE)
                .autoRenew(true)
                .build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Optional.of(ownerSubscription));
        when(subscriptionRepository.save(ownerSubscription)).thenReturn(ownerSubscription);

        Subscription result = subscriptionService.cancelSubscription("owner@test.com", "OWNER_FEATURED");

        assertEquals(30L, result.getId());
        assertEquals(false, result.getAutoRenew());
        verify(subscriptionRepository).findFirstDetailedActiveSubscriptionByType(eq(1L), any(), eq(MembershipPlanType.OWNER_FEATURED));
        verify(subscriptionRepository).save(ownerSubscription);
    }
}
