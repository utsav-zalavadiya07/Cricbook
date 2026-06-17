package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class TournamentFixtureScheduleRequest {
    private Long turfId;
    private Long slotId;
    private LocalDate matchDate;
    private LocalTime startTime;
    private String scheduleNote;
}
