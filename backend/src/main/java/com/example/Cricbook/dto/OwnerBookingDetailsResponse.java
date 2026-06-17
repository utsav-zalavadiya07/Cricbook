package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerBookingDetailsResponse {
    private BookingResponse booking;
    private String latestPaymentStatus;
    private String latestCancellationStatus;
    private List<OwnerBookingTimelineResponse> timeline;
}
