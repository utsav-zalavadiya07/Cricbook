package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class TeamLineupRequest {
    private Long matchId;
    private Long userId;
    private Integer position;
    private String lineupRole;
}
