package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CareerPlatformRequests {

    private CareerPlatformRequests() {
    }

    @Data
    public static class ProfileRequest {
        private String playerName;
        private String playerEmail;
        private String phone;
        private String sport;
        private String specialization;
        private String playerRole;
        private String city;
        private String ageGroup;
        private String profilePhotoUrl;
        private String bio;
        private String statisticsSummary;
        private String certificatesSummary;
        private Boolean scoutOptIn;
        private String visibility;
    }

    @Data
    public static class AchievementRequest {
        private String title;
        private String achievementType;
        private String issuer;
        private LocalDate eventDate;
        private String evidenceUrl;
        private String notes;
    }

    @Data
    public static class TimelineRequest {
        private String eventType;
        private String title;
        private LocalDate eventDate;
        private String summary;
    }

    @Data
    public static class VideoRequest {
        private String category;
        private String title;
        private String videoUrl;
    }

    @Data
    public static class ScoutRequest {
        private String scoutName;
        private String email;
        private String phone;
        private String organizationName;
        private String clubType;
        private String sportSpecialization;
        private String region;
        private String credentialsUrl;
    }

    @Data
    public static class OpportunityRequest {
        private String opportunityType;
        private String title;
        private String organizationName;
        private String sport;
        private String category;
        private String location;
        private String description;
        private String eligibility;
        private Integer capacity;
        private LocalDate deadline;
        private LocalDateTime startsAt;
        private BigDecimal scholarshipAmount;
        private String status;
    }

    @Data
    public static class ApplicationRequest {
        private Long profileId;
        private String statement;
        private String documentsUrl;
    }

    @Data
    public static class StatusRequest {
        private String status;
        private String notes;
        private Integer score;
    }

    @Data
    public static class EvaluationRequest {
        private String evaluatorName;
        private Integer techniqueScore;
        private Integer fitnessScore;
        private Integer disciplineScore;
        private Integer awarenessScore;
        private String decision;
        private String notes;
    }

    @Data
    public static class ScoutActivityRequest {
        private Long profileId;
        private String actionType;
        private String notes;
    }

    @Data
    public static class RecommendationRequest {
        private Long profileId;
        private String recommenderName;
        private String organization;
        private String content;
    }
}
