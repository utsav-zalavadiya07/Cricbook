package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JoinWaitlistRequest {
    private Long slotId;
    private LocalDate date;
    private String city;
    private Integer priority;
}
