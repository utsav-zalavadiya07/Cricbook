package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class CreateLiveScoreRequest {
    private Long fixtureId;
    private Long publicMatchId;
    private String title;
    private String teamAName;
    private String teamBName;
    private String battingTeam;
    private Integer maxOvers;
}
