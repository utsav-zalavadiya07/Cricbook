package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreBowlingRowResponse {
    private String playerName;
    private String overs;
    private Integer balls;
    private Integer runsConceded;
    private Integer wickets;
    private Double economy;
}
