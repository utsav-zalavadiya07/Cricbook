package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerFinanceDashboardResponse {
    private BigDecimal todayRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal lifetimeRevenue;
    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netCollectedRevenue;
    private BigDecimal commissionRate;
    private BigDecimal commissionDue;
    private BigDecimal gstRate;
    private BigDecimal gstOnCommission;
    private BigDecimal netPayable;
    private BigDecimal pendingSettlementAmount;
    private BigDecimal paidSettlementAmount;
    private long paidBookings;
    private long refundedPayments;
    private long pendingSettlements;
    private long paidSettlements;
    private List<RevenuePoint> revenueTrend;
    private List<TurfRevenue> turfRevenue;
    private List<SettlementItem> settlements;
    private List<TransactionItem> transactions;
    private String insight;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenuePoint {
        private LocalDate date;
        private BigDecimal revenue;
        private long payments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TurfRevenue {
        private Long turfId;
        private String turfName;
        private BigDecimal revenue;
        private long paidBookings;
        private BigDecimal averageBookingValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementItem {
        private Long id;
        private Long bookingId;
        private BigDecimal grossAmount;
        private BigDecimal commission;
        private BigDecimal netAmount;
        private String status;
        private String reference;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionItem {
        private Long paymentId;
        private Long bookingId;
        private Long turfId;
        private String turfName;
        private String playerName;
        private BigDecimal amount;
        private BigDecimal refundedAmount;
        private String status;
        private LocalDateTime paidAt;
    }
}
