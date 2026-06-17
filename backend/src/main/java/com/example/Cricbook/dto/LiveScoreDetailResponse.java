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
public class LiveScoreDetailResponse {
    private LiveScoreResponse score;
    private List<ScoreEventResponse> events;
    private List<LiveScoreInningsResponse> innings;
    private List<LiveScoreBattingRowResponse> battingScorecard;
    private List<LiveScoreBowlingRowResponse> bowlingScorecard;
    private List<LiveScoreOverSummaryResponse> recentOvers;
    private List<LiveScoreFallOfWicketResponse> fallOfWickets;
    private String resultSummary;
    private String winnerTeamName;
    private String verificationStatus;
    private String streamUrl;
    private String shareUrl;
    private List<String> auditTrail;

    // Live Scoreboard Components
    private String strikerName;
    private Integer strikerRuns;
    private Integer strikerBalls;
    private Integer strikerFours;
    private Integer strikerSixes;

    private String nonStrikerName;
    private Integer nonStrikerRuns;
    private Integer nonStrikerBalls;
    private Integer nonStrikerFours;
    private Integer nonStrikerSixes;

    private String bowlerName;
    private Double bowlerOvers;
    private Integer bowlerRuns;
    private Integer bowlerWickets;

    private Integer partnershipRuns;
    private Integer partnershipBalls;

    private Double requiredRunRate;
}
