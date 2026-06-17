package com.example.Cricbook.dto;

import com.example.Cricbook.enums.CollectionStatus;
import com.example.Cricbook.enums.PaymentSplitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class SplitPaymentDtos {
    @Data
    public static class CreateSplitRequest {
        private Long bookingId;
        private String splitType = "EQUAL";
        private LocalDate dueDate;
        private List<SplitShareRequest> shares;
    }

    @Data
    public static class SplitShareRequest {
        private Long userId;
        private String loginId;
        private BigDecimal amount;
    }

    @Data
    public static class PayShareRequest {
        private Long splitId;
        private String paymentReference;
        private Boolean useWallet = false;
    }

    @Data
    public static class PaymentRuleRequest {
        private Integer minimumPercentage;
        private Boolean autoCancel;
        private Boolean fullPaymentRequired;
        private Integer reminderHoursBefore;
    }

    @Data
    public static class RecurringCollectionRequest {
        private Long teamId;
        private BigDecimal amount;
        private String frequency;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitPaymentResponse {
        private Long bookingId;
        private TeamCollectionResponse collection;
        private List<PaymentSplitResponse> splits;
        private SplitAnalyticsResponse analytics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSplitResponse {
        private Long id;
        private Long bookingId;
        private String turfName;
        private LocalDate bookingDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Long userId;
        private String playerName;
        private BigDecimal amount;
        private PaymentSplitStatus status;
        private LocalDate dueDate;
        private LocalDateTime paidAt;
        private String paymentLink;
        private String paymentReference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamCollectionResponse {
        private Long id;
        private Long bookingId;
        private Long captainId;
        private String captainName;
        private BigDecimal totalAmount;
        private BigDecimal collectedAmount;
        private BigDecimal pendingAmount;
        private Long paidPlayers;
        private Long pendingPlayers;
        private Integer collectionRate;
        private LocalDate dueDate;
        private CollectionStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRuleResponse {
        private Long id;
        private Long ownerId;
        private Integer minimumPercentage;
        private Boolean autoCancel;
        private Boolean fullPaymentRequired;
        private Integer reminderHoursBefore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecurringCollectionResponse {
        private Long id;
        private Long teamId;
        private BigDecimal amount;
        private String frequency;
        private Boolean active;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitAnalyticsResponse {
        private Long totalSplits;
        private Long paidSplits;
        private Long pendingSplits;
        private BigDecimal totalCollected;
        private BigDecimal totalPending;
        private Integer collectionSuccessRate;
        private String insight;
    }
}
