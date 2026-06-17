package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
public class SlotResponse {
    private Long id;
    private Long turfId;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private Boolean isActive;
}
