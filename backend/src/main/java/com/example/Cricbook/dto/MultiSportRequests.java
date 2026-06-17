package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public final class MultiSportRequests {

    private MultiSportRequests() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportConfigRequest {
        private String name;
        private String code;
        private String icon;
        private String rules;
        private Boolean active;
        private Integer displayOrder;
        private Integer minPlayers;
        private Integer maxPlayers;
        private String scoringType;
        private String matchType;
        private String rankingType;
        private Boolean teamBased;
        private Boolean tournamentEnabled;
        private Boolean statisticsEnabled;
        private String configurationJson;
        private String metadataJson;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportStatusRequest {
        private Boolean active;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportTurfMappingRequest {
        private Long sportId;
        private String sportCode;
        private Long turfId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportBookingValidationRequest {
        private Long turfId;
        private String sportCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportTeamValidationRequest {
        private String sportCode;
        private Integer players;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SportRuleSimulationRequest {
        private String sportCode;
        private String matchType;
        private Integer playersPerSide;
        private Map<String, Object> payload;
    }
}
