package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreInningsResponse {
    private Integer inningsNumber;
    private String battingTeamName;
    private Integer runs;
    private Integer wickets;
    private Integer ballsBowled;
    private String overs;
    private Integer extras;
}
