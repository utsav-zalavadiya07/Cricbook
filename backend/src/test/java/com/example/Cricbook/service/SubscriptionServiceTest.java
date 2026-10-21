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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(subscriptionRepository.findDetailedActiveSubscriptionsByType(eq(1L), eq(MembershipStatus.ACTIVE), any(), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(Collections.emptyList());

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
        assertEquals(new BigDecimal("253.39"), invoiceCaptor.getValue().getAmount());
        assertEquals(new BigDecimal("45.61"), invoiceCaptor.getValue().getTaxAmount());
        assertNotNull(invoiceCaptor.getValue().getInvoiceUrl());
        assertTrue(invoiceCaptor.getValue().getInvoiceUrl().startsWith("/api/v1/billing/invoices/number/INV-"));
        verify(taxRecordRepository, times(1)).save(any(TaxRecord.class));
        verify(revenueRecordRepository, times(1)).save(any(RevenueRecord.class));
    }

    @Test
    void startSubscriptionRejectsRepurchasingSameActivePlan() {
        User user = User.builder().id(1L).email("user@test.com").roles(Collections.emptySet()).build();
        MembershipPlan proPlan = MembershipPlan.builder()
                .id(2L)
                .name("PRO")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .build();

        Subscription activeSub = Subscription.builder()
                .id(10L)
                .user(user)
                .plan(proPlan)
                .status(MembershipStatus.ACTIVE)
                .startDate(java.time.LocalDateTime.now())
                .endDate(java.time.LocalDateTime.now().plusMonths(1))
                .build();

        when(userRepository.findByEmailOrPhone("user@test.com")).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findDetailedActiveSubscriptionsByType(eq(1L), eq(MembershipStatus.ACTIVE), any(), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(List.of(activeSub));

        com.example.Cricbook.exception.ApiException ex = assertThrows(
                com.example.Cricbook.exception.ApiException.class,
                () -> subscriptionService.startSubscription("user@test.com", 2L, null)
        );

        assertTrue(ex.getMessage().contains("You already have an active PRO plan"));
        verify(walletService, never()).debitWallet(any(), any(), any(), any());
    }

    @Test
    void startSubscriptionRejectsDowngradingWhileActivePlanRunning() {
        User user = User.builder().id(1L).email("user@test.com").roles(Collections.emptySet()).build();
        MembershipPlan elitePlan = MembershipPlan.builder()
                .id(3L)
                .name("ELITE")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(599))
                .durationDays(30)
                .build();

        MembershipPlan proPlan = MembershipPlan.builder()
                .id(2L)
                .name("PRO")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .build();

        Subscription activeEliteSub = Subscription.builder()
                .id(11L)
                .user(user)
                .plan(elitePlan)
                .status(MembershipStatus.ACTIVE)
                .startDate(java.time.LocalDateTime.now())
                .endDate(java.time.LocalDateTime.now().plusMonths(1))
                .build();

        when(userRepository.findByEmailOrPhone("user@test.com")).thenReturn(Optional.of(user));
        when(planRepository.findById(2L)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findDetailedActiveSubscriptionsByType(eq(1L), eq(MembershipStatus.ACTIVE), any(), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(List.of(activeEliteSub));

        com.example.Cricbook.exception.ApiException ex = assertThrows(
                com.example.Cricbook.exception.ApiException.class,
                () -> subscriptionService.startSubscription("user@test.com", 2L, null)
        );

        assertTrue(ex.getMessage().contains("You cannot switch to a lower plan (PRO)"));
        verify(walletService, never()).debitWallet(any(), any(), any(), any());
    }

    @Test
    void startSubscriptionAllowsUpgradeAndReplacesExistingPlan() {
        User user = User.builder().id(1L).email("user@test.com").roles(Collections.emptySet()).build();
        MembershipPlan proPlan = MembershipPlan.builder()
                .id(2L)
                .name("PRO")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .build();

        MembershipPlan elitePlan = MembershipPlan.builder()
                .id(3L)
                .name("ELITE")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(599))
                .durationDays(30)
                .build();

        Subscription activeProSub = Subscription.builder()
                .id(10L)
                .user(user)
                .plan(proPlan)
                .status(MembershipStatus.ACTIVE)
                .startDate(java.time.LocalDateTime.now())
                .endDate(java.time.LocalDateTime.now().plusMonths(1))
                .build();

        Wallet wallet = Wallet.builder().user(user).balance(BigDecimal.valueOf(1000)).build();

        when(userRepository.findByEmailOrPhone("user@test.com")).thenReturn(Optional.of(user));
        when(planRepository.findById(3L)).thenReturn(Optional.of(elitePlan));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(revenueExperimentService.getAssignedVariant(1L)).thenReturn("A");
        when(subscriptionRepository.findDetailedActiveSubscriptionsByType(eq(1L), eq(MembershipStatus.ACTIVE), any(), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(List.of(activeProSub));

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            if (s.getId() == null) s.setId(12L);
            return s;
        });

        Subscription result = subscriptionService.startSubscription("user@test.com", 3L, null);

        assertNotNull(result);
        assertEquals(12L, result.getId());
        assertEquals("ELITE", result.getPlan().getName());
        assertEquals(MembershipStatus.ACTIVE, result.getStatus());
        // Verify previous active subscription was cancelled
        assertEquals(MembershipStatus.CANCELLED, activeProSub.getStatus());
        verify(subscriptionRepository).save(activeProSub);
        verify(walletService, times(1)).debitWallet(eq(user), eq(BigDecimal.valueOf(599)), eq("SUBSCRIPTION_CHARGE"), anyString());
    }

    @Test
    void startFeaturedOwnerSubscriptionRequiresTurf() {
        User owner = User.builder().id(2L).email("owner@test.com").roles(Collections.emptySet()).build();
        MembershipPlan plan = MembershipPlan.builder()
                .id(10L)
                .name("BUSINESS")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.valueOf(1499))
                .durationDays(30)
                .featuredListing(true)
                .build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));

        com.example.Cricbook.exception.ApiException exception = assertThrows(
                com.example.Cricbook.exception.ApiException.class,
                () -> subscriptionService.startSubscription("owner@test.com", 10L, null)
        );

        assertEquals("Select a turf to receive the featured listing benefit", exception.getMessage());
        verifyNoInteractions(turfRepository, walletService);
    }

    @Test
    void startFeaturedOwnerSubscriptionLinksTurfUntilPlanExpiry() {
        User owner = User.builder().id(2L).email("owner@test.com").roles(Collections.emptySet()).build();
        Turf turf = Turf.builder().id(7L).name("Central Cricket Arena").owner(owner).build();
        MembershipPlan plan = MembershipPlan.builder()
                .id(10L)
                .name("BUSINESS")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.valueOf(1499))
                .durationDays(30)
                .featuredListing(true)
                .build();
        Wallet wallet = Wallet.builder().user(owner).balance(BigDecimal.valueOf(2000)).build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(planRepository.findById(10L)).thenReturn(Optional.of(plan));
        when(turfRepository.findById(7L)).thenReturn(Optional.of(turf));
        when(subscriptionRepository.findDetailedActiveSubscriptionsByType(
                eq(2L), eq(MembershipStatus.ACTIVE), any(), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Collections.emptyList());
        when(revenueExperimentService.getAssignedVariant(2L)).thenReturn("A");
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(wallet));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = subscriptionService.startSubscription("owner@test.com", 10L, 7L);

        assertEquals(turf, result.getTurf());
        assertEquals(result.getEndDate(), turf.getFeaturedUntil());
        verify(turfRepository).save(turf);
        verify(walletService).debitWallet(owner, BigDecimal.valueOf(1499), "SUBSCRIPTION_CHARGE", "Purchased BUSINESS membership");
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

    @Test
    void setAutoRenewEnablesAutoRenewal() {
        User user = User.builder().id(1L).email("user@test.com").roles(Collections.emptySet()).build();
        MembershipPlan plan = MembershipPlan.builder()
                .id(2L)
                .name("PRO")
                .type(MembershipPlanType.USER_PREMIUM)
                .price(BigDecimal.valueOf(299))
                .durationDays(30)
                .build();
        Subscription subscription = Subscription.builder()
                .id(40L)
                .user(user)
                .plan(plan)
                .status(MembershipStatus.ACTIVE)
                .autoRenew(false)
                .build();

        when(userRepository.findByEmailOrPhone("user@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(eq(1L), any(), eq(MembershipPlanType.USER_PREMIUM)))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        Subscription result = subscriptionService.setAutoRenew("user@test.com", "USER_PREMIUM", true);

        assertEquals(40L, result.getId());
        assertTrue(result.getAutoRenew());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void setAutoRenewUsesOwnerPlanTypeForOwnerBillingControls() {
        User owner = User.builder().id(2L).email("owner@test.com").roles(Collections.emptySet()).build();
        MembershipPlan ownerPlan = MembershipPlan.builder()
                .id(10L)
                .name("ENTERPRISE")
                .type(MembershipPlanType.OWNER_FEATURED)
                .price(BigDecimal.valueOf(4999))
                .durationDays(30)
                .build();
        Subscription ownerSubscription = Subscription.builder()
                .id(41L)
                .user(owner)
                .plan(ownerPlan)
                .status(MembershipStatus.ACTIVE)
                .autoRenew(true)
                .build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(subscriptionRepository.findFirstDetailedActiveSubscriptionByType(
                eq(2L), any(), eq(MembershipPlanType.OWNER_FEATURED)))
                .thenReturn(Optional.of(ownerSubscription));
        when(subscriptionRepository.save(ownerSubscription)).thenReturn(ownerSubscription);

        Subscription result = subscriptionService.setAutoRenew("owner@test.com", "OWNER_FEATURED", false);

        assertEquals(41L, result.getId());
        assertEquals(false, result.getAutoRenew());
        verify(subscriptionRepository).findFirstDetailedActiveSubscriptionByType(
                eq(2L), any(), eq(MembershipPlanType.OWNER_FEATURED));
        verify(subscriptionRepository).save(ownerSubscription);
    }
}
