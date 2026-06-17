package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SportsOsRequests {

    @Data
    public static class FacilityRequest {
        private String name;
        private String facilityType;
        private String ownershipType;
        private String jurisdictionLevel;
        private String stateName;
        private String districtName;
        private String cityName;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Integer capacity;
        private Integer activeSlots;
        private Boolean publicBookingEnabled;
        private String ownerDepartment;
        private String images;
    }

    @Data
    public static class FacilityStatusRequest {
        private String status;
        private String maintenanceStatus;
        private BigDecimal utilizationPercent;
        private Boolean publicBookingEnabled;
    }

    @Data
    public static class CensusSubmissionRequest {
        private String cycleCode;
        private String jurisdictionLevel;
        private String stateName;
        private String districtName;
        private String submittedBy;
        private Integer playerCount;
        private Integer coachCount;
        private Integer academyCount;
        private Integer facilityCount;
        private Integer schoolCount;
        private Integer universityCount;
    }

    @Data
    public static class GrantApplicationRequest {
        private String applicantName;
        private String applicantType;
        private String programType;
        private String stateName;
        private String districtName;
        private BigDecimal requestedAmount;
        private String supportingDocuments;
    }

    @Data
    public static class GrantReviewRequest {
        private String action;
        private BigDecimal approvedAmount;
        private String reviewNote;
    }

    @Data
    public static class GrantDisbursementRequest {
        private String stageName;
        private BigDecimal amount;
        private String note;
        private LocalDateTime disbursedAt;
    }

    @Data
    public static class AthleteDevelopmentRequest {
        private Long sportsIdentityId;
        private String athleteName;
        private String schoolOrInstitution;
        private String sport;
        private String ageGroup;
        private String districtName;
        private String stateName;
        private String currentLevel;
        private String selectionStatus;
        private String selectionHistory;
        private Integer progressScore;
    }

    @Data
    public static class VolunteerRequest {
        private String fullName;
        private String email;
        private String phone;
        private String stateName;
        private String districtName;
        private String category;
        private String verificationStatus;
    }

    @Data
    public static class VolunteerAssignmentRequest {
        private String assignmentType;
        private Long assignmentRefId;
        private String title;
        private String districtName;
    }

    @Data
    public static class InspectionRequest {
        private Long facilityId;
        private String inspectorName;
        private String inspectionType;
        private Integer score;
        private String safetyStatus;
        private String maintenanceStatus;
        private String recommendation;
        private String status;
        private LocalDateTime scheduledAt;
        private LocalDateTime completedAt;
    }
}
