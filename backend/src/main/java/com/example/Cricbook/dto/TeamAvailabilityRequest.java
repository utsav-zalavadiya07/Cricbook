package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamAvailabilityStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeamAvailabilityRequest {
    private LocalDate date;
    private TeamAvailabilityStatus status;
    private String note;
}
