package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMatchStatsResponse {
    private Long teamId;
    private String teamName;
    private Long matchesPlayed;
    private Long wins;
    private Long losses;
    private Long draws;
    private Integer winPercentage;
    private Long pendingChallenges;
    private Long upcomingMatches;
    private List<PublicMatchResponse> recentMatches;
    private List<RivalryResponse> rivalries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RivalryResponse {
        private Long opponentTeamId;
        private String opponentTeamName;
        private Long matches;
        private Long wins;
        private Long losses;
    }
}
