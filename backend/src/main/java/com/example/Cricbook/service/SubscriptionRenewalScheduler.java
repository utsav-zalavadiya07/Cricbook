package com.example.Cricbook.service;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.MembershipStatus;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionRenewalRepository renewalRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaxRecordRepository taxRecordRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final NotificationService notificationService;

    // Runs every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void processExpiredRenewals() {
        log.info("Starting automated subscription renewal engine check...");
        
        List<Subscription> expiring = subscriptionRepository.findByStatusAndEndDateBefore(
                MembershipStatus.ACTIVE, LocalDateTime.now());

        log.info("Found {} subscriptions requiring renewal/expiration checks.", expiring.size());

        for (Subscription sub : expiring) {
            processSubscriptionRenewal(sub);
        }
    }

    @Transactional
    public void triggerRenewalsForTesting() {
        log.info("Manually triggering subscription renewal engine check...");
        processExpiredRenewals();
    }

    private void processSubscriptionRenewal(Subscription sub) {
        User user = sub.getUser();
        MembershipPlan plan = sub.getPlan();

        if (Boolean.FALSE.equals(sub.getAutoRenew()) || plan.getPrice().compareTo(BigDecimal.ZERO) == 0) {
            // Gracefully expire if auto-renew is false or plan is FREE
            sub.setStatus(MembershipStatus.EXPIRED);
            subscriptionRepository.save(sub);
            
            notificationService.pushNotification(
                    user.getId(),
                    user.getEmail(),
                    "Subscription Expired",
                    "Your " + plan.getName() + " subscription has expired."
            );
            log.info("Subscription #{} for user {} has expired gracefully.", sub.getId(), user.getEmail());
            return;
        }

        // Count previous failed attempts for this renewal cycle
        List<SubscriptionRenewal> history = renewalRepository.findBySubscriptionIdOrderByAttemptedAtDesc(sub.getId());
        long failedAttempts = history.stream()
                .filter(r -> "FAILED".equals(r.getStatus()))
                .count();

        int currentAttempt = (int) failedAttempts + 1;

        try {
            Wallet wallet = walletRepository.findByUserId(user.getId())
                    .orElseGet(() -> walletRepository.save(Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .build()));

            BigDecimal price = plan.getPrice();

            if (wallet.getBalance().compareTo(price) >= 0) {
                // Charge successful
                walletService.debitWallet(user, price, "RENEWAL_CHARGE", "Auto-renew " + plan.getName() + " subscription");

                LocalDateTime renewalStart = LocalDateTime.now().withNano(0);
                LocalDateTime renewalEnd;
                if (plan.getDurationDays() > 0 && plan.getDurationDays() % 30 == 0 && plan.getDurationDays() <= 365) {
                    renewalEnd = renewalStart.plusMonths(plan.getDurationDays() / 30);
                } else {
                    renewalEnd = renewalStart.plusDays(plan.getDurationDays());
                }

                sub.setStartDate(renewalStart);
                sub.setEndDate(renewalEnd);
                sub.setStatus(MembershipStatus.ACTIVE);
                subscriptionRepository.save(sub);

                // Log Renewal Success
                renewalRepository.save(SubscriptionRenewal.builder()
                        .subscription(sub)
                        .attempt(currentAttempt)
                        .status("SUCCESS")
                        .build());

                // Generate Invoice
                BigDecimal taxAmount = price.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
                String invoiceNum = "INV-REN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                Invoice invoice = invoiceRepository.save(Invoice.builder()
                        .invoiceNumber(invoiceNum)
                        .user(user)
                        .subscription(sub)
                        .amount(price)
                        .taxAmount(taxAmount)
                        .status("PAID")
                        .invoiceUrl(invoiceUrl(invoiceNum))
                        .build());

                // Create Tax Record
                taxRecordRepository.save(TaxRecord.builder()
                        .invoice(invoice)
                        .gstAmount(taxAmount)
                        .taxRate(BigDecimal.valueOf(0.18))
                        .build());

                // Record Revenue
                revenueRecordRepository.save(RevenueRecord.builder()
                        .source("SUBSCRIPTION")
                        .amount(price)
                        .build());

                notificationService.pushNotification(
                        user.getId(),
                        user.getEmail(),
                        "Subscription Renewed",
                        "Your " + plan.getName() + " subscription has been successfully renewed!"
                );
                log.info("Subscription #{} for user {} renewed successfully on attempt #{}", sub.getId(), user.getEmail(), currentAttempt);

            } else {
                // Insufficient funds
                throw new Exception("Insufficient wallet funds for auto-renew");
            }

        } catch (Exception e) {
            log.warn("Subscription renewal charge failed for user {}: {}", user.getEmail(), e.getMessage());

            // Log Renewal Failure
            renewalRepository.save(SubscriptionRenewal.builder()
                    .subscription(sub)
                    .attempt(currentAttempt)
                    .status("FAILED")
                    .build());

            if (currentAttempt >= 3) {
                // Mark Expired after 3 failures
                sub.setStatus(MembershipStatus.EXPIRED);
                subscriptionRepository.save(sub);

                notificationService.pushNotification(
                        user.getId(),
                        user.getEmail(),
                        "Subscription Expired",
                        "Your " + plan.getName() + " subscription has expired due to recurring payment failures."
                );
                log.info("Subscription #{} for user {} marked as EXPIRED after 3 failed attempts.", sub.getId(), user.getEmail());
            } else {
                // Send reminder and retry warning
                notificationService.pushNotification(
                        user.getId(),
                        user.getEmail(),
                        "Subscription Renewal Failed",
                        "Auto-renew charge for " + plan.getName() + " failed (Attempt " + currentAttempt + "/3). We will retry in 24 hours. Please add money to your wallet."
                );
                log.info("Subscription #{} for user {} retry scheduled (Attempt {}/3 failed).", sub.getId(), user.getEmail(), currentAttempt);
            }
        }
    }

    private String invoiceUrl(String invoiceNumber) {
        return "/api/v1/billing/invoices/number/" + invoiceNumber + "/download";
    }
}
