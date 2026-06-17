package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamMemberRole;
import com.example.Cricbook.enums.TeamMemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private TeamMemberRole role;
    private TeamMemberStatus status;
    private LocalDateTime createdAt;
}
