package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class PlayerAvailabilityRequest {
    private String availableDays;
    private String preferredWindow;
    private String homeCity;
    private Boolean canTravel;
    private String notes;
}
