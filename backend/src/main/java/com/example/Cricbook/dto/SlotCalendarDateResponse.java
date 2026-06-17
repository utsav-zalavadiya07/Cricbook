package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotCalendarDateResponse {
    private LocalDate date;
    private long totalSlots;
    private long availableSlots;
    private long bookedSlots;
    private long blockedSlots;
    private String status;
}
