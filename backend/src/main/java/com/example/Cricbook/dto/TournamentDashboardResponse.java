package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentDashboardResponse {
    private TournamentResponse tournament;
    private List<TournamentRegistrationResponse> registrations;
    private List<TournamentFixtureResponse> fixtures;
    private List<TournamentStandingResponse> standings;
    private List<TournamentRewardResponse> rewards;
    private List<FeedEventResponse> feed;
    private Integer approvedTeams;
    private Integer pendingTeams;
    private Integer waitlistTeams;
    private Integer completedFixtures;
    private Integer upcomingFixtures;
    private BigDecimal registrationRevenue;
}
