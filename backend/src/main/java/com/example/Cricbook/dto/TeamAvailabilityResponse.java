package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamAvailabilityStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TeamAvailabilityResponse {
    private Long id;
    private Long teamId;
    private Long userId;
    private String userName;
    private LocalDate date;
    private TeamAvailabilityStatus status;
    private String note;
}
