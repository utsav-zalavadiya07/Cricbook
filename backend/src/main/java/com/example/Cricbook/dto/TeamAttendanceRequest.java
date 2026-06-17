package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamAttendanceStatus;
import lombok.Data;

@Data
public class TeamAttendanceRequest {
    private Long matchId;
    private Long userId;
    private TeamAttendanceStatus status;
    private String note;
}
