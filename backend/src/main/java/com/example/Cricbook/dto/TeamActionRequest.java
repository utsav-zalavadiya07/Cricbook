package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamMemberRole;
import lombok.Data;

@Data
public class TeamActionRequest {
    private Long userId;
    private TeamMemberRole role;
}
