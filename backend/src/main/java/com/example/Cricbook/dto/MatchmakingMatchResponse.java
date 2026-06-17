package com.example.Cricbook.dto;

import com.example.Cricbook.enums.PublicMatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingMatchResponse {
    private Long id;
    private String title;
    private String city;
    private String location;
    private LocalDate matchDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPlayers;
    private Integer joinedPlayers;
    private Integer slotsLeft;
    private String skillLevel;
    private Boolean autoApprove;
    private Boolean trustedOnly;
    private PublicMatchStatus status;
    private Integer matchScore;
    private String recommendationReason;
    private PublicMatchResponse match;
    private List<MatchmakingCandidateResponse> candidates;
}
