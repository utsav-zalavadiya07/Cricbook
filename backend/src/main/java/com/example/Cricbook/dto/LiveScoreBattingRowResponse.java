package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreBattingRowResponse {
    private String playerName;
    private Integer runs;
    private Integer balls;
    private Integer fours;
    private Integer sixes;
    private Double strikeRate;
    private Boolean out;
    private String dismissal;
}
