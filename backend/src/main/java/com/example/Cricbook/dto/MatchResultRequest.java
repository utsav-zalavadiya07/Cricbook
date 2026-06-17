package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class MatchResultRequest {
    private Long winnerTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private String resultMargin;
    private String resultSummary;
}
