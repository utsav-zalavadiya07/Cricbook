package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FanEconomyRequests {

    @Data
    public static class FanProfileRequest {
        private String displayName;
        private String email;
        private String city;
        private String favoriteSport;
        private String favoriteTeam;
        private String favoritePlayer;
        private String avatarUrl;
    }

    @Data
    public static class FanClubRequest {
        private String name;
        private String sport;
        private String clubType;
        private String linkedEntityType;
        private Long linkedEntityId;
        private String city;
        private String visibility;
        private String description;
    }

    @Data
    public static class JoinFanClubRequest {
        private Long fanProfileId;
    }

    @Data
    public static class CollectibleRequest {
        private String title;
        private String collectibleType;
        private String rarity;
        private String sourceModule;
        private Long sourceRefId;
        private Integer supplyLimit;
        private String mediaUrl;
        private String credentialReference;
    }

    @Data
    public static class ClaimCollectibleRequest {
        private Long fanProfileId;
    }

    @Data
    public static class PredictionGameRequest {
        private String title;
        private String sport;
        private String targetType;
        private Long targetRefId;
        private String predictionType;
        private Integer pointsForCorrect;
        private LocalDateTime deadlineAt;
        private String settlementRule;
    }

    @Data
    public static class PredictionEntryRequest {
        private Long fanProfileId;
        private String predictionValue;
    }

    @Data
    public static class PredictionScoreRequest {
        private String resultStatus;
        private String settlementNote;
    }

    @Data
    public static class CreatorProfileRequest {
        private String displayName;
        private String email;
        private String city;
        private String creatorCategory;
        private String bio;
        private String profileUrl;
    }

    @Data
    public static class CreatorContentRequest {
        private String title;
        private String contentType;
        private String sport;
        private String mediaUrl;
        private String visibility;
        private Boolean sponsored;
    }

    @Data
    public static class MarketplaceListingRequest {
        private Long ownerFanProfileId;
        private Long collectibleOwnershipId;
        private BigDecimal price;
        private String currency;
    }

    @Data
    public static class MarketplacePurchaseRequest {
        private Long buyerFanProfileId;
    }

    @Data
    public static class WatchPartyRequest {
        private String title;
        private Long hostFanProfileId;
        private String sport;
        private Long matchId;
        private Long tournamentId;
        private String platform;
        private Integer capacity;
        private LocalDateTime scheduledAt;
        private String accessMode;
    }

    @Data
    public static class JoinWatchPartyRequest {
        private Long fanProfileId;
    }
}
