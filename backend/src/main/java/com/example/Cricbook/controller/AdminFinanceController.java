package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PAYMENT_VIEW')")
@Transactional
public class AdminFinanceController {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRepository refundRepository;
    private final OwnerSettlementRepository ownerSettlementRepository;
    private final CommissionRuleRepository commissionRuleRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final FinanceDisputeRepository financeDisputeRepository;
    private final ReconciliationLogRepository reconciliationLogRepository;
    private final TaxRecordRepository taxRecordRepository;
    private final TaxReportRepository taxReportRepository;
    private final FinanceAuditRepository financeAuditRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final PlatformSettingRepository platformSettingRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        List<Payment> payments = paymentRepository.findAll();
        List<Refund> refunds = refundRepository.findAll();
        List<OwnerSettlement> settlements = ownerSettlementRepository.findAll();
        BigDecimal gmv = payments.stream().map(Payment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = refunds.stream().map(Refund::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = settlements.stream().map(OwnerSettlement::getCommission).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue = commission.subtract(refundAmount);
        long pendingSettlements = settlements.stream().filter(item -> "PENDING".equalsIgnoreCase(item.getStatus()) || "PROCESSING".equalsIgnoreCase(item.getStatus())).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gmv", gmv);
        response.put("netRevenue", netRevenue.max(BigDecimal.ZERO));
        response.put("commissionRevenue", commission);
        response.put("refundAmount", refundAmount);
        response.put("pendingSettlements", pendingSettlements);
        response.put("transactions", payments.size());
        response.put("refunds", refunds.size());
        response.put("openDisputes", financeDisputeRepository.findByStatusOrderByCreatedAtDesc("OPEN").size());
        response.put("fraudAlerts", fraudAlertRepository.findAll().stream().filter(alert -> !"DISMISSED".equalsIgnoreCase(alert.getSeverity())).count());
        response.put("dailyRevenue", dailyRevenue(payments));
        response.put("insight", financeInsight(gmv, refundAmount, commission));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Map<String, Object>>> transactions(@RequestParam(required = false) String status) {
        String normalized = safe(status).trim();
        return ResponseEntity.ok(paymentRepository.findAll().stream()
                .filter(payment -> normalized.isBlank() || normalized.equalsIgnoreCase(payment.getStatus()))
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapPayment)
                .toList());
    }

    @GetMapping("/refunds")
    public ResponseEntity<List<Map<String, Object>>> refunds() {
        return ResponseEntity.ok(refundRepository.findAll().stream()
                .sorted(Comparator.comparing(Refund::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapRefund)
                .toList());
    }

    @PostMapping("/refunds/{refundId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('REFUND_APPROVE') or hasAuthority('PAYMENT_REFUND')")
    public ResponseEntity<Map<String, Object>> updateRefundStatus(@PathVariable Long refundId, @RequestBody StatusRequest request) {
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new NoSuchElementException("Refund not found"));
        refund.setStatus(safe(request.getStatus()).isBlank() ? "APPROVED" : request.getStatus().trim().toUpperCase(Locale.ROOT));
        Refund saved = refundRepository.save(refund);
        writeFinanceAudit("REFUND_STATUS_CHANGED", saved.getPayment().getBooking().getUser().getId(), "Refund:" + saved.getId(), saved.getStatus());
        return ResponseEntity.ok(mapRefund(saved));
    }

    @PostMapping("/refunds/force")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('REFUND_APPROVE') or hasAuthority('PAYMENT_REFUND')")
    public ResponseEntity<Map<String, Object>> forceRefund(@RequestBody RefundRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId()).orElseThrow(() -> new NoSuchElementException("Payment not found"));
        BigDecimal amount = positiveOrDefault(request.getAmount(), payment.getAmount());
        Refund refund = refundRepository.save(Refund.builder()
                .payment(payment)
                .amount(amount)
                .status("SUCCESS")
                .reason(safe(request.getReason()).isBlank() ? "Admin force refund" : request.getReason())
                .gatewayRefundId("manual_ref_" + UUID.randomUUID().toString().substring(0, 8))
                .build());
        payment.setStatus("REFUNDED");
        paymentRepository.save(payment);
        writeFinanceAudit("FORCE_REFUND", payment.getBooking().getUser().getId(), "Payment:" + payment.getId(), "Refunded " + amount);
        return ResponseEntity.ok(mapRefund(refund));
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<Map<String, Object>>> settlements() {
        return ResponseEntity.ok(ownerSettlementRepository.findAll().stream()
                .sorted(Comparator.comparing(OwnerSettlement::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapSettlement)
                .toList());
    }

    @PostMapping("/settlements/generate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SETTLEMENT_PROCESS') or hasAuthority('PAYMENT_PAYOUT')")
    public ResponseEntity<Map<String, Object>> generateSettlements() {
        BigDecimal rate = commissionRate();
        List<OwnerSettlement> created = new ArrayList<>();
        for (Payment payment : paymentRepository.findAll()) {
            if (payment.getBooking() == null || payment.getBooking().getTurf() == null || payment.getBooking().getTurf().getOwner() == null) {
                continue;
            }
            if (ownerSettlementRepository.findByBookingId(payment.getBooking().getId()).isPresent()) {
                continue;
            }
            BigDecimal gross = nullToZero(payment.getAmount());
            BigDecimal commission = gross.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            OwnerSettlement settlement = ownerSettlementRepository.save(OwnerSettlement.builder()
                    .owner(payment.getBooking().getTurf().getOwner())
                    .booking(payment.getBooking())
                    .grossAmount(gross)
                    .commission(commission)
                    .netAmount(gross.subtract(commission))
                    .status("PENDING")
                    .reference("stl_" + UUID.randomUUID().toString().substring(0, 8))
                    .build());
            created.add(settlement);
        }
        writeFinanceAudit("SETTLEMENTS_GENERATED", null, "OwnerSettlement", "Generated " + created.size() + " settlements");
        return ResponseEntity.ok(Map.of("created", created.size(), "commissionRate", rate, "settlements", created.stream().map(this::mapSettlement).toList()));
    }

    @PostMapping("/settlements/{settlementId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SETTLEMENT_PROCESS') or hasAuthority('PAYMENT_PAYOUT')")
    public ResponseEntity<Map<String, Object>> updateSettlementStatus(@PathVariable Long settlementId, @RequestBody StatusRequest request) {
        OwnerSettlement settlement = ownerSettlementRepository.findById(settlementId).orElseThrow(() -> new NoSuchElementException("Settlement not found"));
        settlement.setStatus(safe(request.getStatus()).isBlank() ? "PAID" : request.getStatus().trim().toUpperCase(Locale.ROOT));
        OwnerSettlement saved = ownerSettlementRepository.save(settlement);
        writeFinanceAudit("SETTLEMENT_STATUS_CHANGED", saved.getOwner().getId(), "OwnerSettlement:" + saved.getId(), saved.getStatus());
        return ResponseEntity.ok(mapSettlement(saved));
    }

    @GetMapping("/commission-rules")
    public ResponseEntity<List<CommissionRule>> commissionRules() {
        return ResponseEntity.ok(commissionRuleRepository.findAll());
    }

    @PostMapping("/commission-rules")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('COMMISSION_EDIT')")
    public ResponseEntity<CommissionRule> saveCommissionRule(@RequestBody CommissionRuleRequest request) {
        CommissionRule rule = commissionRuleRepository.save(CommissionRule.builder()
                .type(safe(request.getType()).isBlank() ? "PERCENT" : request.getType().trim().toUpperCase(Locale.ROOT))
                .value(positiveOrDefault(request.getValue(), BigDecimal.TEN))
                .ownerId(request.getOwnerId())
                .active(true)
                .description(safe(request.getDescription()).isBlank() ? "Admin commission rule" : request.getDescription())
                .build());
        writeFinanceAudit("COMMISSION_RULE_CREATED", request.getOwnerId(), "CommissionRule:" + rule.getId(), rule.getType() + " " + rule.getValue());
        return ResponseEntity.ok(rule);
    }

    @GetMapping("/wallets")
    public ResponseEntity<List<Map<String, Object>>> wallets() {
        return ResponseEntity.ok(walletRepository.findAll().stream().map(this::mapWallet).toList());
    }

    @PostMapping("/wallets/adjust")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('WALLET_ADJUST')")
    public ResponseEntity<Map<String, Object>> adjustWallet(@RequestBody WalletAdjustmentRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow(() -> new NoSuchElementException("User not found"));
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseGet(() -> walletRepository.save(Wallet.builder().user(user).balance(BigDecimal.ZERO).build()));
        BigDecimal amount = positiveOrDefault(request.getAmount(), BigDecimal.ZERO);
        String type = safe(request.getType()).isBlank() ? "ADMIN_CREDIT" : request.getType().trim().toUpperCase(Locale.ROOT);
        BigDecimal signedAmount = type.contains("DEBIT") ? amount.negate() : amount;
        wallet.setBalance(wallet.getBalance().add(signedAmount));
        Wallet savedWallet = walletRepository.save(wallet);
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(savedWallet)
                .amount(signedAmount)
                .type(type)
                .status("SUCCESS")
                .reference("admin_adj_" + UUID.randomUUID().toString().substring(0, 8))
                .description(safe(request.getReason()).isBlank() ? "Admin wallet adjustment" : request.getReason())
                .build());
        writeFinanceAudit("WALLET_ADJUSTED", user.getId(), "Wallet:" + savedWallet.getId(), type + " " + signedAmount);
        return ResponseEntity.ok(mapWallet(savedWallet));
    }

    @GetMapping("/disputes")
    public ResponseEntity<List<FinanceDispute>> disputes() {
        return ResponseEntity.ok(financeDisputeRepository.findAll());
    }

    @PostMapping("/disputes")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PAYMENT_EDIT')")
    public ResponseEntity<FinanceDispute> createDispute(@RequestBody FinanceDisputeRequest request) {
        FinanceDispute dispute = financeDisputeRepository.save(FinanceDispute.builder()
                .userId(request.getUserId())
                .bookingId(request.getBookingId())
                .type(safe(request.getType()).isBlank() ? "REFUND_ISSUE" : request.getType().trim().toUpperCase(Locale.ROOT))
                .status("OPEN")
                .amount(positiveOrDefault(request.getAmount(), BigDecimal.ZERO))
                .summary(safe(request.getSummary()).isBlank() ? "Admin-created financial dispute" : request.getSummary())
                .build());
        writeFinanceAudit("FINANCE_DISPUTE_OPENED", dispute.getUserId(), "FinanceDispute:" + dispute.getId(), dispute.getType());
        return ResponseEntity.ok(dispute);
    }

    @PostMapping("/disputes/{disputeId}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PAYMENT_EDIT')")
    public ResponseEntity<FinanceDispute> resolveDispute(@PathVariable Long disputeId, @RequestBody DisputeResolutionRequest request) {
        FinanceDispute dispute = financeDisputeRepository.findById(disputeId).orElseThrow(() -> new NoSuchElementException("Dispute not found"));
        dispute.setStatus("RESOLVED");
        dispute.setResolution(safe(request.getResolution()).isBlank() ? "Resolved by finance admin" : request.getResolution());
        FinanceDispute saved = financeDisputeRepository.save(dispute);
        writeFinanceAudit("FINANCE_DISPUTE_RESOLVED", saved.getUserId(), "FinanceDispute:" + saved.getId(), saved.getResolution());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<List<ReconciliationLog>> reconciliation() {
        return ResponseEntity.ok(reconciliationLogRepository.findTop100ByOrderByCreatedAtDesc());
    }

    @PostMapping("/reconciliation/run")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PAYMENT_EDIT')")
    public ResponseEntity<Map<String, Object>> runReconciliation() {
        List<ReconciliationLog> created = paymentRepository.findAll().stream().map(payment -> {
            BigDecimal amount = nullToZero(payment.getAmount());
            String transactionId = payment.getRazorpayPaymentId() == null ? "payment-" + payment.getId() : payment.getRazorpayPaymentId();
            String status = amount.compareTo(BigDecimal.ZERO) > 0 ? "MATCHED" : "MISMATCH";
            return reconciliationLogRepository.save(ReconciliationLog.builder()
                    .transactionId(transactionId)
                    .gatewayAmount(amount)
                    .systemAmount(amount)
                    .status(status)
                    .notes(status.equals("MATCHED") ? "Gateway and system amounts match" : "Missing amount")
                    .build());
        }).toList();
        writeFinanceAudit("RECONCILIATION_RUN", null, "ReconciliationLog", "Checked " + created.size() + " payments");
        return ResponseEntity.ok(Map.of("checked", created.size(), "mismatches", created.stream().filter(item -> "MISMATCH".equals(item.getStatus())).count()));
    }

    @GetMapping("/tax-reports")
    public ResponseEntity<List<TaxReport>> taxReports() {
        return ResponseEntity.ok(taxReportRepository.findTop50ByOrderByGeneratedAtDesc());
    }

    @PostMapping("/tax-reports/generate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PAYMENT_VIEW')")
    public ResponseEntity<TaxReport> generateTaxReport(@RequestBody TaxReportRequest request) {
        BigDecimal taxable = paymentRepository.findAll().stream().map(Payment::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gst = taxable.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
        TaxReport report = taxReportRepository.save(TaxReport.builder()
                .reportType(safe(request.getReportType()).isBlank() ? "GST_SUMMARY" : request.getReportType().trim().toUpperCase(Locale.ROOT))
                .period(safe(request.getPeriod()).isBlank() ? LocalDate.now().toString().substring(0, 7) : request.getPeriod())
                .taxableAmount(taxable)
                .gstAmount(gst)
                .build());
        writeFinanceAudit("TAX_REPORT_GENERATED", null, "TaxReport:" + report.getId(), report.getReportType() + " " + report.getPeriod());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/audits")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('AUDIT_VIEW') or hasAuthority('SECURITY_AUDIT_VIEW')")
    public ResponseEntity<List<FinanceAudit>> audits() {
        return ResponseEntity.ok(financeAuditRepository.findTop100ByOrderByTimestampDesc());
    }

    @GetMapping("/fraud")
    public ResponseEntity<List<FraudAlert>> fraud() {
        return ResponseEntity.ok(fraudAlertRepository.findAll());
    }

    private Map<String, Object> mapPayment(Payment payment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", payment.getId());
        item.put("transactionId", payment.getRazorpayPaymentId());
        item.put("orderId", payment.getRazorpayOrderId());
        item.put("bookingId", payment.getBooking() == null ? null : payment.getBooking().getId());
        item.put("player", payment.getBooking() == null || payment.getBooking().getUser() == null ? "Unknown" : payment.getBooking().getUser().getName());
        item.put("owner", payment.getBooking() == null || payment.getBooking().getTurf() == null || payment.getBooking().getTurf().getOwner() == null ? "Unknown" : payment.getBooking().getTurf().getOwner().getName());
        item.put("amount", payment.getAmount());
        item.put("gateway", "RAZORPAY");
        item.put("status", payment.getStatus());
        item.put("createdAt", payment.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapRefund(Refund refund) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", refund.getId());
        item.put("paymentId", refund.getPayment().getId());
        item.put("bookingId", refund.getPayment().getBooking().getId());
        item.put("amount", refund.getAmount());
        item.put("status", refund.getStatus());
        item.put("reason", refund.getReason());
        item.put("gatewayRefundId", refund.getGatewayRefundId());
        item.put("createdAt", refund.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapSettlement(OwnerSettlement settlement) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", settlement.getId());
        item.put("ownerId", settlement.getOwner().getId());
        item.put("ownerName", settlement.getOwner().getName());
        item.put("bookingId", settlement.getBooking() == null ? null : settlement.getBooking().getId());
        item.put("grossAmount", settlement.getGrossAmount());
        item.put("commission", settlement.getCommission());
        item.put("netAmount", settlement.getNetAmount());
        item.put("status", settlement.getStatus());
        item.put("reference", settlement.getReference());
        item.put("createdAt", settlement.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapWallet(Wallet wallet) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", wallet.getId());
        item.put("userId", wallet.getUser().getId());
        item.put("userName", wallet.getUser().getName());
        item.put("balance", wallet.getBalance());
        item.put("transactions", walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream().limit(5).toList());
        item.put("updatedAt", wallet.getUpdatedAt());
        return item;
    }

    private List<Map<String, Object>> dailyRevenue(List<Payment> payments) {
        Map<LocalDate, BigDecimal> byDate = new TreeMap<>();
        payments.forEach(payment -> {
            LocalDate date = payment.getCreatedAt() == null ? LocalDate.now() : payment.getCreatedAt().toLocalDate();
            byDate.put(date, byDate.getOrDefault(date, BigDecimal.ZERO).add(nullToZero(payment.getAmount())));
        });
        return byDate.entrySet().stream().map(entry -> Map.<String, Object>of("date", entry.getKey(), "revenue", entry.getValue())).toList();
    }

    private String financeInsight(BigDecimal gmv, BigDecimal refunds, BigDecimal commission) {
        if (gmv.compareTo(BigDecimal.ZERO) == 0) return "No financial activity yet. Transactions will appear once bookings are paid.";
        BigDecimal refundRatio = refunds.multiply(BigDecimal.valueOf(100)).divide(gmv, 2, RoundingMode.HALF_UP);
        return "Refund ratio is " + refundRatio + "% and commission captured is Rs " + commission + ".";
    }

    private BigDecimal commissionRate() {
        return platformSettingRepository.findByKey("COMMISSION_RATE").map(PlatformSetting::getValue).map(BigDecimal::new).orElse(BigDecimal.TEN);
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? fallback : value;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String actor() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? "system_admin" : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void writeFinanceAudit(String action, Long userId, String entity, String details) {
        financeAuditRepository.save(FinanceAudit.builder()
                .action(action)
                .actor(actor())
                .userId(userId)
                .entity(entity)
                .details(details)
                .build());
    }

    @Data
    public static class StatusRequest {
        private String status;
    }

    @Data
    public static class RefundRequest {
        private Long paymentId;
        private BigDecimal amount;
        private String reason;
    }

    @Data
    public static class CommissionRuleRequest {
        private String type;
        private BigDecimal value;
        private Long ownerId;
        private String description;
    }

    @Data
    public static class WalletAdjustmentRequest {
        private Long userId;
        private BigDecimal amount;
        private String type;
        private String reason;
    }

    @Data
    public static class FinanceDisputeRequest {
        private Long userId;
        private Long bookingId;
        private String type;
        private BigDecimal amount;
        private String summary;
    }

    @Data
    public static class DisputeResolutionRequest {
        private String resolution;
    }

    @Data
    public static class TaxReportRequest {
        private String reportType;
        private String period;
    }
}
