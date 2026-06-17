package com.example.Cricbook.dto;

import com.example.Cricbook.enums.LiveMatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreResponse {
    private Long id;
    private Long fixtureId;
    private Long publicMatchId;
    private Long scorerId;
    private String scorerName;
    private String title;
    private String teamAName;
    private String teamBName;
    private String battingTeam;
    private String bowlingTeam;
    private Integer innings;
    private Integer runs;
    private Integer wickets;
    private Integer ballsBowled;
    private String overs;
    private Integer extras;
    private Integer targetRuns;
    private Integer maxOvers;
    private Double runRate;
    private Integer projectedScore;
    private LiveMatchStatus status;
    private Boolean locked;
    private Boolean tie;
    private Boolean superOver;
    private String winnerTeamName;
    private String resultSummary;
    private String shareUrl;
    private LocalDateTime updatedAt;
}
