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
public class ScoreEventResponse {
    private Long id;
    private Integer eventSequence;
    private Integer runs;
    private Integer extraRuns;
    private String extraType;
    private Boolean wicket;
    private String wicketType;
    private String batter;
    private String bowler;
    private String note;
    private LocalDateTime createdAt;
}
