package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SportsSuperAppRequests {

    @Data
    public static class IdentityRequest {
        private Long userId;
        private String displayName;
        private String primaryEmail;
        private String phone;
        private String city;
        private String primarySport;
        private String ageGroup;
        private String guardianEmail;
        private String profileVisibility;
        private String linkedModules;
    }

    @Data
    public static class PassportRequest {
        private String visibility;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class WalletLedgerRequest {
        private BigDecimal amount;
        private String sourceModule;
        private String referenceType;
        private String referenceId;
        private String note;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class WalletTransferRequest {
        private Long toIdentityId;
        private BigDecimal amount;
        private String sourceModule;
        private String referenceType;
        private String referenceId;
        private String note;
    }

    @Data
    public static class MembershipRequest {
        private String tier;
        private String billingCycle;
        private BigDecimal price;
        private LocalDateTime startsAt;
        private LocalDateTime expiresAt;
        private Boolean autoRenew;
        private String benefitsSummary;
    }

    @Data
    public static class RewardRequest {
        private Integer points;
        private String sourceModule;
        private String note;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class ReputationRequest {
        private String eventType;
        private String sourceModule;
        private Integer impact;
        private String note;
    }

    @Data
    public static class TimelineRequest {
        private String eventType;
        private String sourceModule;
        private String title;
        private LocalDateTime occurredAt;
        private String visibility;
        private String metadata;
    }

    @Data
    public static class FamilyAccountRequest {
        private Long parentIdentityId;
        private Long childIdentityId;
        private String guardianEmail;
        private String relationshipType;
        private Boolean approvalRequired;
    }

    @Data
    public static class SportsCreditRequest {
        private Integer credits;
        private String eventType;
        private String sourceModule;
        private String note;
    }
}
