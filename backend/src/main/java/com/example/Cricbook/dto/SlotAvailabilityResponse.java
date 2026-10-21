package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotAvailabilityResponse {
    private Long id;
    private SlotResponse slot;
    private Long slotId;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private BigDecimal listingPrice;
    private LocalDate date;
    private Boolean isAvailable;
    private Boolean isBlocked;
    private Boolean available;
    private Boolean blocked;
    private Boolean locked;
    private String status;
    private String demandTag;
}
