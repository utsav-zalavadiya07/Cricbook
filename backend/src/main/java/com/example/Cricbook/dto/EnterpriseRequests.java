package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EnterpriseRequests {

    @Data
    public static class BusinessRequest {
        private String name;
        private String businessType;
        private String logoUrl;
        private String status;
        private Long ownerId;
        private Long tenantId;
    }

    @Data
    public static class CenterRequest {
        private Long businessId;
        private String name;
        private String city;
        private String state;
        private Double latitude;
        private Double longitude;
        private Long managerId;
        private String status;
    }

    @Data
    public static class StaffAssignmentRequest {
        private Long businessId;
        private Long centerId;
        private Long staffId;
        private String role;
        private String permissions;
        private Boolean active;
    }

    @Data
    public static class FranchiseRequest {
        private Long businessId;
        private Long centerId;
        private String name;
        private String status;
        private BigDecimal revenueSharePercent;
        private LocalDate agreementStart;
        private LocalDate agreementEnd;
        private String complianceStatus;
    }
}
