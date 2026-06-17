package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeamLineupResponse {
    private Long id;
    private Long teamId;
    private Long matchId;
    private Long userId;
    private String userName;
    private Integer position;
    private String lineupRole;
}
