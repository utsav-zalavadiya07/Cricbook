package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreFallOfWicketResponse {
    private Integer wicketNumber;
    private Integer score;
    private String over;
    private String batter;
    private String wicketType;
}
