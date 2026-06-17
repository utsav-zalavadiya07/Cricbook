package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBatchBookingRequest {
    private Long turfId;
    private List<Long> slotIds;
    private LocalDate bookingDate;
    private String sport;
    private String sportCode;
}
