package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class SponsorshipRequests {
    private SponsorshipRequests() {
    }

    @Data
    public static class BrandRequest {
        private String companyName;
        private String brandType;
        private String gstNumber;
        private String website;
        private String email;
        private String phone;
        private String city;
        private String logoUrl;
        private String documentUrl;
    }

    @Data
    public static class OpportunityRequest {
        private String title;
        private String opportunityType;
        private String description;
        private String city;
        private String sportCategory;
        private Integer audienceSize;
        private BigDecimal amount;
        private String benefits;
        private String ownerReference;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class ProposalRequest {
        private Long brandId;
        private String message;
        private BigDecimal proposedAmount;
        private String deliverables;
        private String documentUrl;
    }

    @Data
    public static class CampaignRequest {
        private Long brandId;
        private String name;
        private String campaignType;
        private String placement;
        private String targetAudience;
        private String city;
        private String creativeUrl;
        private String destinationUrl;
        private BigDecimal dailyBudget;
        private BigDecimal lifetimeBudget;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
    }

    @Data
    public static class ContractRequest {
        private Long brandId;
        private Long proposalId;
        private String contractNumber;
        private String contractType;
        private BigDecimal value;
        private String documentUrl;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
    }

    @Data
    public static class LeadRequest {
        private Long brandId;
        private Long campaignId;
        private String leadType;
        private String name;
        private String email;
        private String phone;
        private String city;
        private String notes;
        private BigDecimal estimatedValue;
    }

    @Data
    public static class EventRequest {
        private String eventType;
        private String userReference;
        private String sessionId;
        private String leadReference;
        private BigDecimal cost;
    }

    @Data
    public static class InvoiceRequest {
        private Long brandId;
        private Long contractId;
        private String invoiceNumber;
        private String revenueCategory;
        private BigDecimal subtotal;
        private BigDecimal gstAmount;
        private String status;
        private String notes;
    }

    @Data
    public static class StatusRequest {
        private String status;
        private String notes;
    }
}
