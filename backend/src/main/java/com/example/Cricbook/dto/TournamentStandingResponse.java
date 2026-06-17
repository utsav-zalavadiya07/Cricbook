package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentStandingResponse {
    private Long teamId;
    private String teamName;
    private Integer played;
    private Integer wins;
    private Integer losses;
    private Integer draws;
    private Integer points;
    private Integer runsFor;
    private Integer runsAgainst;
    private Integer netRuns;
    private Double netRunRate;
}
