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
public class PlayerAvailabilityResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String availableDays;
    private String preferredWindow;
    private String homeCity;
    private Boolean canTravel;
    private String notes;
    private LocalDateTime updatedAt;
}
