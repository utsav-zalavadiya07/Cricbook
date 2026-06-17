package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class DeveloperPlatformRequests {

    private DeveloperPlatformRequests() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeveloperRegistrationRequest {
        private String name;
        private String email;
        private String companyName;
        private String website;
        private String plan;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeveloperStatusRequest {
        private String status;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationRequest {
        private Long developerId;
        private String name;
        private String environment;
        private String callbackUrl;
        private List<String> scopes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiKeyRequest {
        private Long applicationId;
        private String environment;
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OAuthTokenRequest {
        private String grantType;
        private String clientId;
        private String clientSecret;
        private String refreshToken;
        private String authorizationCode;
        private List<String> scopes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookRequest {
        private Long applicationId;
        private String url;
        private List<String> eventTypes;
        private String secret;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceListingRequest {
        private Long developerId;
        private String title;
        private String category;
        private String description;
        private String website;
        private String pricingModel;
        private java.math.BigDecimal revenueSharePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusRequest {
        private String status;
        private String notes;
    }
}
