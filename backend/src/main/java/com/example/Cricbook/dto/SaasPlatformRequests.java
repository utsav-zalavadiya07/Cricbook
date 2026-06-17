package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SaasPlatformRequests {

    @Data
    public static class TenantRequest {
        private String name;
        private String domain;
        private String tenantType;
        private String countryCode;
        private String regionCode;
        private String defaultLocale;
        private String currency;
        private String timezone;
        private String lifecycleStage;
        private String status;
    }

    @Data
    public static class TenantStatusRequest {
        private String status;
        private String lifecycleStage;
        private String reason;
    }

    @Data
    public static class BrandingRequest {
        private Long tenantId;
        private String appName;
        private String logoUrl;
        private String faviconUrl;
        private String primaryColor;
        private String secondaryColor;
        private String fontFamily;
        private Boolean published;
    }

    @Data
    public static class CustomDomainRequest {
        private Long tenantId;
        private String domain;
        private String routingMode;
    }

    @Data
    public static class FeatureToggleRequest {
        private Long tenantId;
        private String featureKey;
        private Boolean enabled;
        private String rolloutStrategy;
        private String notes;
    }

    @Data
    public static class SubscriptionRequest {
        private Long tenantId;
        private String planCode;
        private String billingCycle;
        private BigDecimal price;
        private String currency;
        private Boolean autoRenew;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class InvoiceRequest {
        private Long tenantId;
        private Long subscriptionId;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private String currency;
        private LocalDate dueDate;
    }

    @Data
    public static class InvoicePaymentRequest {
        private BigDecimal amount;
        private String paymentReference;
    }

    @Data
    public static class MarketplaceModuleRequest {
        private String moduleCode;
        private String name;
        private String category;
        private String pricingModel;
        private BigDecimal price;
        private String currency;
        private String certificationStatus;
    }

    @Data
    public static class ModuleInstallationRequest {
        private Long tenantId;
        private Long moduleId;
        private String licenseType;
    }

    @Data
    public static class AppBuildRequest {
        private Long tenantId;
        private String appType;
        private String platform;
        private String version;
    }

    @Data
    public static class ComplianceRequest {
        private Long tenantId;
        private String regulation;
        private String requestType;
        private String subjectEmail;
        private String notes;
    }

    @Data
    public static class ResellerRequest {
        private String name;
        private String email;
        private String regionCode;
        private BigDecimal commissionPercent;
        private Long parentResellerId;
    }
}
