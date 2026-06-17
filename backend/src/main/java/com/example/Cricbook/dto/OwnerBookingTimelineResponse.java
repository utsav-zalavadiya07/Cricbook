package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerBookingTimelineResponse {
    private Long id;
    private Long bookingId;
    private String eventType;
    private String performedBy;
    private String details;
    private LocalDateTime createdAt;
}
