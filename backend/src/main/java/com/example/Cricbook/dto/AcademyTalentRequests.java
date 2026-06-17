package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class AcademyTalentRequests {

    private AcademyTalentRequests() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcademyRequest {
        private String name;
        private String description;
        private String sportsOffered;
        private String city;
        private String address;
        private String website;
        private String email;
        private String phone;
        private String logoUrl;
        private String documentUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoachRequest {
        private Long academyId;
        private String name;
        private String email;
        private String phone;
        private String sport;
        private String city;
        private Integer experienceYears;
        private BigDecimal pricePerSession;
        private String certifications;
        private String achievements;
        private String profilePhotoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgramRequest {
        private Long academyId;
        private Long coachId;
        private String title;
        private String sport;
        private String level;
        private String description;
        private Integer durationWeeks;
        private Integer capacity;
        private BigDecimal price;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchRequest {
        private Long programId;
        private String name;
        private String scheduleLabel;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer capacity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentRequest {
        private Long batchId;
        private String studentName;
        private String studentEmail;
        private Integer studentAge;
        private BigDecimal amountPaid;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceRequest {
        private Long enrollmentId;
        private LocalDate sessionDate;
        private String status;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessmentRequest {
        private Long enrollmentId;
        private Long coachId;
        private Integer battingScore;
        private Integer bowlingScore;
        private Integer fieldingScore;
        private Integer fitnessScore;
        private Integer disciplineScore;
        private String recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoRequest {
        private Long enrollmentId;
        private String category;
        private String videoUrl;
        private String coachComments;
        private String improvementAreas;
        private String recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TalentProfileRequest {
        private Long enrollmentId;
        private String city;
        private String highlights;
        private Integer tournamentImpactScore;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateRequest {
        private Long enrollmentId;
        private String certificateType;
        private String pdfUrl;
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
