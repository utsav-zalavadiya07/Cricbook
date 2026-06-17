package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class SlotWaitlistResponse {
    private Long id;
    private Long slotId;
    private Long turfId;
    private String turfName;
    private String city;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer priority;
    private Boolean active;
    private LocalDateTime createdAt;
}
