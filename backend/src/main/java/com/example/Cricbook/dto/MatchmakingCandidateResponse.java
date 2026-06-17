package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchmakingCandidateResponse {
    private Long userId;
    private String name;
    private String city;
    private String position;
    private String preferredTime;
    private String skillLevel;
    private Integer trustScore;
    private Double rating;
    private Integer matches;
    private Integer recommendationScore;
    private String reason;
}
