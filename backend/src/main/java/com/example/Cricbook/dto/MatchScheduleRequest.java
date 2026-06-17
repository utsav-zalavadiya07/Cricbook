package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchScheduleRequest {
    private Long turfId;
    private LocalDate matchDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String note;
}
