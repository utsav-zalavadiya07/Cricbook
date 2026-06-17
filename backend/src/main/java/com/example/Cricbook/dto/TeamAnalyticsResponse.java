package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TeamAnalyticsResponse {
    private Long totalTeams;
    private Long activeMembers;
    private Long availabilityResponses;
    private Long polls;
    private BigDecimal teamWalletBalance;
    private Integer averageAttendanceRate;
    private List<TeamWorkspaceResponse> mostActiveTeams;
}
