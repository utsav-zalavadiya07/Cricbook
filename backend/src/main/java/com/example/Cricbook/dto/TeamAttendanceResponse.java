package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamAttendanceStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamAttendanceResponse {
    private Long id;
    private Long teamId;
    private Long matchId;
    private Long userId;
    private String userName;
    private TeamAttendanceStatus status;
    private String note;
}
