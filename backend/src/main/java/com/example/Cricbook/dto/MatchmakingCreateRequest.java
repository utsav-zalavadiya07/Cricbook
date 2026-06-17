package com.example.Cricbook.dto;

import com.example.Cricbook.enums.MatchFormat;
import com.example.Cricbook.enums.MatchVisibility;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MatchmakingCreateRequest {
    private Long turfId;
    private String title;
    private String sport;
    private String city;
    private String location;
    private LocalDate matchDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPlayers;
    private Integer requiredPlayers;
    private String skillLevel;
    private Boolean autoApprove;
    private Boolean trustedOnly;
    private MatchVisibility visibility;
    private MatchFormat format;
    private Integer overs;
    private Double entryFee;
    private Long homeTeamId;
    private Long awayTeamId;
    private String notes;
}
