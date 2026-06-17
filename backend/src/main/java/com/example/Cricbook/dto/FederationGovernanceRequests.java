package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FederationGovernanceRequests {

    @Data
    public static class BodyRequest {
        private Long parentBodyId;
        private String name;
        private String bodyType;
        private String sport;
        private String region;
        private String registrationNumber;
        private String website;
        private String contactEmail;
        private String governanceNotes;
    }

    @Data
    public static class FranchiseRequest {
        private Long federationId;
        private String name;
        private String ownerName;
        private String city;
        private String homeVenue;
        private BigDecimal salaryCap;
    }

    @Data
    public static class PlayerRegistrationRequest {
        private Long federationId;
        private Long careerProfileId;
        private String playerName;
        private String email;
        private String phone;
        private String sport;
        private String city;
        private String ageGroup;
        private LocalDate dateOfBirth;
        private String identityDocumentUrl;
    }

    @Data
    public static class DraftRequest {
        private Long federationId;
        private String name;
        private String sport;
        private String season;
        private String draftOrderFranchiseIds;
        private LocalDateTime startsAt;
    }

    @Data
    public static class DraftSelectionRequest {
        private Long franchiseId;
        private Long playerRegistrationId;
        private String selectionType;
    }

    @Data
    public static class AuctionRequest {
        private Long federationId;
        private Long playerRegistrationId;
        private String auctionName;
        private BigDecimal basePrice;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
    }

    @Data
    public static class BidRequest {
        private Long franchiseId;
        private BigDecimal bidAmount;
        private String bidderName;
    }

    @Data
    public static class TransferRequest {
        private Long federationId;
        private Long playerRegistrationId;
        private Long toFranchiseId;
        private String transferType;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String reason;
        private Boolean transferWindowOpen;
    }

    @Data
    public static class LicenseRequest {
        private Long federationId;
        private String holderType;
        private Long holderId;
        private String licenseType;
        private LocalDate validFrom;
        private LocalDate validUntil;
    }

    @Data
    public static class OfficialRequest {
        private Long federationId;
        private String name;
        private String email;
        private String officialType;
        private String certificationUrl;
        private LocalDate certificationValidUntil;
    }

    @Data
    public static class CaseRequest {
        private Long federationId;
        private String subjectType;
        private Long subjectId;
        private String caseType;
        private String severity;
        private String investigator;
        private String evidenceSummary;
    }

    @Data
    public static class ElectionRequest {
        private Long federationId;
        private String title;
        private String positionName;
        private String candidateNames;
        private LocalDateTime votingOpensAt;
        private LocalDateTime votingClosesAt;
    }

    @Data
    public static class VoteRequest {
        private String voterReference;
        private String candidateName;
    }

    @Data
    public static class StatusRequest {
        private String status;
        private String notes;
        private String penalty;
        private LocalDate penaltyUntil;
    }

    @Data
    public static class RankingRequest {
        private Long federationId;
        private String entityType;
        private Long entityId;
        private String rankingType;
        private String category;
        private Integer points;
        private String calculationNotes;
    }

    @Data
    public static class IntegrityAlertRequest {
        private Long federationId;
        private String alertType;
        private String severity;
        private String subjectReference;
        private String evidence;
        private Integer riskScore;
    }
}
