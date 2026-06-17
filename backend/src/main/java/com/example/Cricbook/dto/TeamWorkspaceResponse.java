package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TeamWorkspaceResponse {
    private Long teamId;
    private String teamName;
    private String city;
    private String sport;
    private String visibility;
    private String teamType;
    private String bannerUrl;
    private String goals;
    private Integer memberCount;
    private Integer availableToday;
    private Integer maybeToday;
    private Integer unavailableToday;
    private BigDecimal teamBalance;
    private Integer attendanceRate;
    private List<TeamMemberResponse> members;
    private List<TeamAvailabilityResponse> availability;
    private List<TeamLineupResponse> lineup;
    private List<TeamPollResponse> polls;
    private List<TeamAnnouncementResponse> announcements;
    private List<TeamFinanceResponse> finances;
    private List<TeamAttendanceResponse> attendance;
}
