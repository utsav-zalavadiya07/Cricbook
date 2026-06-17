package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDiscoverResponse {
    private Long userId;
    private String name;
    private String email;
    private String city;
    private String preferredTime;
    private String battingStyle;
    private String bowlingStyle;
    private String position;
    private String experience;
    private String skillLevel;
    private Integer trustScore;
    private Integer networkScore;
    private Integer matchScore;
    private String connectionStatus;
    private String recommendationReason;
    private Double rating;
    private Integer matches;
    private Integer wins;
    private PlayerAvailabilityResponse availability;
}
