package com.example.Cricbook.service;

import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.entity.TaxRecord;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.Wallet;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.repository.InvoiceRepository;
import com.example.Cricbook.repository.RevenueRecordRepository;
import com.example.Cricbook.repository.SubscriptionRenewalRepository;
import com.example.Cricbook.repository.SubscriptionRepository;
import com.example.Cricbook.repository.TaxRecordRepository;
import com.example.Cricbook.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionRenewalSchedulerTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionRenewalRepository renewalRepository;
    @Mock private WalletService walletService;
    @Mock private WalletRepository walletRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TaxRecordRepository taxRecordRepository;
    @Mock private RevenueRecordRepository revenueRecordRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SubscriptionRenewalScheduler scheduler;

    @Test
    void renewalKeepsPlanPriceAsGstInclusiveTotal() {
        User user = User.builder().id(1L).email("owner@example.com").build();
        MembershipPlan plan = MembershipPlan.builder()
                .name("ENTERPRISE")
                .price(new BigDecimal("4999.00"))
                .durationDays(30)
                .build();
        Subscription subscription = Subscription.builder()
                .id(15L)
                .user(user)
                .plan(plan)
                .status(MembershipStatus.ACTIVE)
                .autoRenew(true)
                .endDate(LocalDateTime.now().minusMinutes(1))
                .build();
        Wallet wallet = Wallet.builder().user(user).balance(new BigDecimal("6000.00")).build();

        when(subscriptionRepository.findByStatusAndEndDateBefore(eq(MembershipStatus.ACTIVE), any()))
                .thenReturn(List.of(subscription));
        when(renewalRepository.findBySubscriptionIdOrderByAttemptedAtDesc(15L))
                .thenReturn(Collections.emptyList());
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduler.processExpiredRenewals();

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertEquals(new BigDecimal("4236.44"), invoiceCaptor.getValue().getAmount());
        assertEquals(new BigDecimal("762.56"), invoiceCaptor.getValue().getTaxAmount());
        verify(walletService).debitWallet(user, new BigDecimal("4999.00"), "RENEWAL_CHARGE", "Auto-renew ENTERPRISE subscription");

        ArgumentCaptor<TaxRecord> taxRecordCaptor = ArgumentCaptor.forClass(TaxRecord.class);
        verify(taxRecordRepository).save(taxRecordCaptor.capture());
        assertEquals(new BigDecimal("762.56"), taxRecordCaptor.getValue().getGstAmount());
    }
}
