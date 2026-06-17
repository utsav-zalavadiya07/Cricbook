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
public class MatchmakingAnalyticsResponse {
    private Long openMatches;
    private Long fullMatches;
    private Long totalParticipants;
    private Double averageFillRate;
    private List<MatchmakingMatchResponse> mostFillableMatches;
}
