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
public class TrustAnalyticsResponse {
    private long totalProfiles;
    private long elitePlayers;
    private long trustedPlayers;
    private long riskyPlayers;
    private long openReports;
    private List<TrustScoreResponse> riskiestPlayers;
    private List<TrustScoreResponse> mostReliablePlayers;
}
