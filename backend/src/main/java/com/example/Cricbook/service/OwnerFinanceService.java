package com.example.Cricbook.service;

import com.example.Cricbook.dto.OwnerFinanceDashboardResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerFinanceService {
    private static final BigDecimal DEFAULT_COMMISSION_RATE = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_GST_RATE = BigDecimal.valueOf(18);
    private static final Set<String> PAID_PAYMENT_STATUSES = Set.of("SUCCESS", "PAID");
    private static final Set<String> REFUND_DEDUCTION_STATUSES = Set.of("SUCCESS", "APPROVED", "PROCESSED", "REFUNDED");
    private static final Set<String> OPEN_SETTLEMENT_STATUSES = Set.of("PENDING", "PROCESSING", "ON_HOLD");

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OwnerSettlementRepository ownerSettlementRepository;
    private final CommissionRuleRepository commissionRuleRepository;
    private final FinanceAuditRepository financeAuditRepository;

    @Transactional(readOnly = true)
    public OwnerFinanceDashboardResponse getDashboard(String username, LocalDate start, LocalDate end, Long turfId) {
        User owner = findOwner(username);
        FinanceWindow window = resolveWindow(start, end);
        List<Payment> ownerPayments = ownerPayments(owner.getId(), turfId);
        List<Refund> ownerRefunds = ownerRefunds(owner.getId(), turfId);
        BigDecimal commissionRate = commissionRate(owner.getId());
        return buildDashboard(owner, ownerPayments, ownerRefunds, window, turfId, commissionRate);
    }

    @Transactional
    public Map<String, Object> generateSettlements(String username) {
        User owner = findOwner(username);
        BigDecimal commissionRate = commissionRate(owner.getId());
        List<Refund> refunds = ownerRefunds(owner.getId(), null);
        Map<Long, BigDecimal> refundedByPayment = processedRefundsByPayment(refunds);
        List<OwnerSettlement> created = new ArrayList<>();

        for (Payment payment : ownerPayments(owner.getId(), null)) {
            if (!isPaidPayment(payment) || payment.getBooking() == null || payment.getBooking().getId() == null) {
                continue;
            }
            if (ownerSettlementRepository.findByBookingId(payment.getBooking().getId()).isPresent()) {
                continue;
            }
            BigDecimal gross = money(nullToZero(payment.getAmount()).subtract(refundedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO)));
            if (gross.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal commission = percent(gross, commissionRate);
            BigDecimal gst = percent(commission, DEFAULT_GST_RATE);
            OwnerSettlement settlement = ownerSettlementRepository.save(OwnerSettlement.builder()
                    .owner(owner)
                    .booking(payment.getBooking())
                    .grossAmount(gross)
                    .commission(commission)
                    .netAmount(money(gross.subtract(commission).subtract(gst)).max(BigDecimal.ZERO))
                    .status("PENDING")
                    .reference("stl_" + UUID.randomUUID().toString().substring(0, 8))
                    .build());
            created.add(settlement);
        }

        writeFinanceAudit("OWNER_SETTLEMENTS_GENERATED", owner.getId(), "OwnerSettlement", "Generated " + created.size() + " owner settlements");
        OwnerFinanceDashboardResponse dashboard = buildDashboard(owner, ownerPayments(owner.getId(), null), ownerRefunds(owner.getId(), null), resolveWindow(null, null), null, commissionRate);
        return Map.of(
                "created", created.size(),
                "commissionRate", commissionRate,
                "settlements", created.stream().map(this::mapSettlement).toList(),
                "dashboard", dashboard
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(String username, LocalDate start, LocalDate end, Long turfId) {
        OwnerFinanceDashboardResponse dashboard = getDashboard(username, start, end, turfId);
        StringBuilder csv = new StringBuilder();
        csv.append("payment_id,booking_id,turf,player,amount,refunded_amount,status,paid_at\n");
        for (OwnerFinanceDashboardResponse.TransactionItem item : dashboard.getTransactions()) {
            csv.append(value(item.getPaymentId())).append(',')
                    .append(value(item.getBookingId())).append(',')
                    .append(csvText(item.getTurfName())).append(',')
                    .append(csvText(item.getPlayerName())).append(',')
                    .append(value(item.getAmount())).append(',')
                    .append(value(item.getRefundedAmount())).append(',')
                    .append(csvText(item.getStatus())).append(',')
                    .append(csvText(item.getPaidAt()))
                    .append('\n');
        }
        return csv.toString();
    }

    private OwnerFinanceDashboardResponse buildDashboard(
            User owner,
            List<Payment> ownerPayments,
            List<Refund> ownerRefunds,
            FinanceWindow window,
            Long turfId,
            BigDecimal commissionRate
    ) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        Map<Long, BigDecimal> refundedByPayment = processedRefundsByPayment(ownerRefunds);
        List<Payment> paidPayments = ownerPayments.stream().filter(this::isPaidPayment).toList();
        List<Payment> windowPayments = paidPayments.stream()
                .filter(payment -> isInWindow(paymentDate(payment), window.start(), window.end()))
                .toList();
        List<OwnerSettlement> settlements = ownerSettlementRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .filter(settlement -> turfId == null || settlement.getBooking() == null || settlement.getBooking().getTurf() == null || turfId.equals(settlement.getBooking().getTurf().getId()))
                .toList();

        BigDecimal grossRevenue = paymentTotal(windowPayments);
        BigDecimal refundedAmount = windowPayments.stream()
                .map(payment -> refundedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCollected = money(grossRevenue.subtract(refundedAmount));
        BigDecimal commission = percent(netCollected, commissionRate);
        BigDecimal gst = percent(commission, DEFAULT_GST_RATE);
        BigDecimal netPayable = money(netCollected.subtract(commission).subtract(gst));

        BigDecimal todayRevenue = netRevenueForDate(paidPayments, refundedByPayment, today);
        BigDecimal monthlyRevenue = paidPayments.stream()
                .filter(payment -> !paymentDate(payment).isBefore(monthStart) && !paymentDate(payment).isAfter(today))
                .map(payment -> netPaymentAmount(payment, refundedByPayment))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lifetimeRevenue = paidPayments.stream()
                .map(payment -> netPaymentAmount(payment, refundedByPayment))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingSettlementAmount = settlements.stream()
                .filter(settlement -> OPEN_SETTLEMENT_STATUSES.contains(normalize(settlement.getStatus())))
                .map(OwnerSettlement::getNetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidSettlementAmount = settlements.stream()
                .filter(settlement -> "PAID".equals(normalize(settlement.getStatus())))
                .map(OwnerSettlement::getNetAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OwnerFinanceDashboardResponse.builder()
                .todayRevenue(money(todayRevenue))
                .monthlyRevenue(money(monthlyRevenue))
                .lifetimeRevenue(money(lifetimeRevenue))
                .grossRevenue(money(grossRevenue))
                .refundedAmount(money(refundedAmount))
                .netCollectedRevenue(money(netCollected))
                .commissionRate(money(commissionRate))
                .commissionDue(money(commission))
                .gstRate(money(DEFAULT_GST_RATE))
                .gstOnCommission(money(gst))
                .netPayable(netPayable.max(BigDecimal.ZERO))
                .pendingSettlementAmount(money(pendingSettlementAmount))
                .paidSettlementAmount(money(paidSettlementAmount))
                .paidBookings(windowPayments.size())
                .refundedPayments(ownerPayments.stream().filter(payment -> "REFUNDED".equals(normalize(payment.getStatus()))).count())
                .pendingSettlements(settlements.stream().filter(settlement -> OPEN_SETTLEMENT_STATUSES.contains(normalize(settlement.getStatus()))).count())
                .paidSettlements(settlements.stream().filter(settlement -> "PAID".equals(normalize(settlement.getStatus()))).count())
                .revenueTrend(buildRevenueTrend(windowPayments, refundedByPayment, window.start(), window.end()))
                .turfRevenue(buildTurfRevenue(windowPayments, refundedByPayment))
                .settlements(settlements.stream().limit(12).map(this::mapSettlement).toList())
                .transactions(windowPayments.stream().limit(20).map(payment -> mapTransaction(payment, refundedByPayment)).toList())
                .insight(financeInsight(netCollected, refundedAmount, pendingSettlementAmount))
                .build();
    }

    private List<OwnerFinanceDashboardResponse.RevenuePoint> buildRevenueTrend(List<Payment> payments, Map<Long, BigDecimal> refundedByPayment, LocalDate start, LocalDate end) {
        Map<LocalDate, List<Payment>> byDate = payments.stream().collect(Collectors.groupingBy(this::paymentDate));
        return start.datesUntil(end.plusDays(1))
                .map(date -> {
                    List<Payment> daily = byDate.getOrDefault(date, List.of());
                    BigDecimal revenue = daily.stream()
                            .map(payment -> netPaymentAmount(payment, refundedByPayment))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return OwnerFinanceDashboardResponse.RevenuePoint.builder()
                            .date(date)
                            .revenue(money(revenue))
                            .payments(daily.size())
                            .build();
                })
                .toList();
    }

    private List<OwnerFinanceDashboardResponse.TurfRevenue> buildTurfRevenue(List<Payment> payments, Map<Long, BigDecimal> refundedByPayment) {
        return payments.stream()
                .filter(payment -> payment.getBooking() != null && payment.getBooking().getTurf() != null)
                .collect(Collectors.groupingBy(payment -> payment.getBooking().getTurf().getId()))
                .values()
                .stream()
                .map(turfPayments -> {
                    Payment first = turfPayments.get(0);
                    Turf turf = first.getBooking().getTurf();
                    BigDecimal revenue = turfPayments.stream()
                            .map(payment -> netPaymentAmount(payment, refundedByPayment))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal average = turfPayments.isEmpty()
                            ? BigDecimal.ZERO
                            : revenue.divide(BigDecimal.valueOf(turfPayments.size()), 2, RoundingMode.HALF_UP);
                    return OwnerFinanceDashboardResponse.TurfRevenue.builder()
                            .turfId(turf.getId())
                            .turfName(turf.getName())
                            .revenue(money(revenue))
                            .paidBookings(turfPayments.size())
                            .averageBookingValue(money(average))
                            .build();
                })
                .sorted(Comparator.comparing(OwnerFinanceDashboardResponse.TurfRevenue::getRevenue).reversed())
                .toList();
    }

    private OwnerFinanceDashboardResponse.TransactionItem mapTransaction(Payment payment, Map<Long, BigDecimal> refundedByPayment) {
        Booking booking = payment.getBooking();
        Turf turf = booking == null ? null : booking.getTurf();
        User player = booking == null ? null : booking.getUser();
        return OwnerFinanceDashboardResponse.TransactionItem.builder()
                .paymentId(payment.getId())
                .bookingId(booking == null ? null : booking.getId())
                .turfId(turf == null ? null : turf.getId())
                .turfName(turf == null ? "Unknown turf" : turf.getName())
                .playerName(player == null ? "Unknown player" : player.getName())
                .amount(money(nullToZero(payment.getAmount())))
                .refundedAmount(money(refundedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO)))
                .status(payment.getStatus())
                .paidAt(payment.getCreatedAt())
                .build();
    }

    private OwnerFinanceDashboardResponse.SettlementItem mapSettlement(OwnerSettlement settlement) {
        return OwnerFinanceDashboardResponse.SettlementItem.builder()
                .id(settlement.getId())
                .bookingId(settlement.getBooking() == null ? null : settlement.getBooking().getId())
                .grossAmount(money(settlement.getGrossAmount()))
                .commission(money(settlement.getCommission()))
                .netAmount(money(settlement.getNetAmount()))
                .status(settlement.getStatus())
                .reference(settlement.getReference())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }

    private FinanceWindow resolveWindow(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = end == null ? today : end;
        LocalDate rangeStart = start == null ? today.minusDays(29) : start;
        if (rangeStart.isAfter(rangeEnd)) {
            throw ApiException.badRequest("Start date cannot be after end date.");
        }
        if (rangeStart.isAfter(today) || rangeEnd.isAfter(today)) {
            throw ApiException.badRequest("Future date ranges are not allowed for financial reports.");
        }
        return new FinanceWindow(rangeStart, rangeEnd);
    }

    private List<Payment> ownerPayments(Long ownerId, Long turfId) {
        return paymentRepository.findByBookingTurfOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(payment -> turfId == null || payment.getBooking() == null || payment.getBooking().getTurf() == null || turfId.equals(payment.getBooking().getTurf().getId()))
                .toList();
    }

    private List<Refund> ownerRefunds(Long ownerId, Long turfId) {
        return refundRepository.findByPaymentBookingTurfOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(refund -> turfId == null || refund.getPayment() == null || refund.getPayment().getBooking() == null || refund.getPayment().getBooking().getTurf() == null || turfId.equals(refund.getPayment().getBooking().getTurf().getId()))
                .toList();
    }

    private Map<Long, BigDecimal> processedRefundsByPayment(List<Refund> refunds) {
        return refunds.stream()
                .filter(refund -> refund.getPayment() != null && refund.getPayment().getId() != null)
                .filter(refund -> REFUND_DEDUCTION_STATUSES.contains(normalize(refund.getStatus())))
                .collect(Collectors.groupingBy(refund -> refund.getPayment().getId(), Collectors.mapping(refund -> nullToZero(refund.getAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private BigDecimal commissionRate(Long ownerId) {
        return commissionRuleRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(rule -> "PERCENT".equals(normalize(rule.getType())))
                .filter(rule -> rule.getOwnerId() != null && rule.getOwnerId().equals(ownerId))
                .map(CommissionRule::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> commissionRuleRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                        .filter(rule -> "PERCENT".equals(normalize(rule.getType())))
                        .filter(rule -> rule.getOwnerId() == null)
                        .map(CommissionRule::getValue)
                        .filter(Objects::nonNull)
                        .findFirst())
                .orElse(DEFAULT_COMMISSION_RATE);
    }

    private User findOwner(String username) {
        return userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> ApiException.notFound("Owner account was not found."));
    }

    private BigDecimal paymentTotal(List<Payment> payments) {
        return payments.stream().map(payment -> nullToZero(payment.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal netRevenueForDate(List<Payment> payments, Map<Long, BigDecimal> refundedByPayment, LocalDate date) {
        return payments.stream()
                .filter(payment -> paymentDate(payment).equals(date))
                .map(payment -> netPaymentAmount(payment, refundedByPayment))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal netPaymentAmount(Payment payment, Map<Long, BigDecimal> refundedByPayment) {
        return money(nullToZero(payment.getAmount()).subtract(refundedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO))).max(BigDecimal.ZERO);
    }

    private boolean isPaidPayment(Payment payment) {
        return PAID_PAYMENT_STATUSES.contains(normalize(payment.getStatus()));
    }

    private LocalDate paymentDate(Payment payment) {
        if (payment.getCreatedAt() != null) {
            return payment.getCreatedAt().toLocalDate();
        }
        if (payment.getBooking() != null && payment.getBooking().getBookingDate() != null) {
            return payment.getBooking().getBookingDate();
        }
        return LocalDate.now();
    }

    private boolean isInWindow(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal rate) {
        return money(nullToZero(amount).multiply(nullToZero(rate)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal amount) {
        return nullToZero(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String financeInsight(BigDecimal netCollected, BigDecimal refundedAmount, BigDecimal pendingSettlementAmount) {
        if (netCollected.compareTo(BigDecimal.ZERO) == 0) {
            return "No settled revenue in this period. Paid bookings will appear here after successful payment.";
        }
        if (refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Refunds reduced this period by Rs " + money(refundedAmount) + ". Recheck cancellation reasons before payout.";
        }
        if (pendingSettlementAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Pending payout queue is Rs " + money(pendingSettlementAmount) + ". Review settlement status before month end.";
        }
        return "Revenue is clean for this period with no processed refunds deducted.";
    }

    private void writeFinanceAudit(String action, Long userId, String entity, String details) {
        String actor = SecurityContextHolder.getContext().getAuthentication() == null
                ? "owner-finance"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        financeAuditRepository.save(FinanceAudit.builder()
                .action(action)
                .actor(actor)
                .userId(userId)
                .entity(entity)
                .details(details)
                .build());
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String csvText(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private record FinanceWindow(LocalDate start, LocalDate end) {
    }
}
