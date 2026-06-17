package com.example.Cricbook.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchAnalyticsResponse {
    private List<Integer> wormTeamA;
    private List<Integer> wormTeamB;
    private List<PartnershipDto> partnerships;
    private List<PlayerMetricDto> topBatters;
    private List<PlayerMetricDto> topBowlers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnershipDto {
        private String batsman1;
        private String batsman2;
        private Integer runs;
        private Integer balls;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerMetricDto {
        private String playerName;
        private Double metricValue;
        private String metricLabel;
    }
}
