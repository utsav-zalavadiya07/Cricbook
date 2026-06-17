package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class ScoreEventRequest {
    private Integer runs;
    private Integer extraRuns;
    private String extraType;
    private Boolean wicket;
    private String wicketType;
    private String batter;
    private String bowler;
    private String note;
}
