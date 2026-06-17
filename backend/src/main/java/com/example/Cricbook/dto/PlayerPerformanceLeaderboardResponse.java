package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerPerformanceLeaderboardResponse {
    private Long userId;
    private String playerName;
    private String city;
    private Integer rank;
    private String leaderboardType;
    private Double score;
    private String statLabel;
    private Integer matches;
    private Integer runs;
    private Integer wickets;
    private Integer impactScore;
}
