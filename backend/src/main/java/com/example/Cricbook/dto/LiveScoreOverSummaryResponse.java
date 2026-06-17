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
public class LiveScoreOverSummaryResponse {
    private Integer overNumber;
    private String label;
    private Integer runs;
    private Integer wickets;
    private List<ScoreEventResponse> events;
}
