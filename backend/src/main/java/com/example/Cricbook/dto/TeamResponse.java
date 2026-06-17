package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String name;
    private String sport;
    private String city;
    private String description;
    private String logoUrl;
    private TeamVisibility visibility;
    private Integer maxMembers;
    private Integer memberCount;
    private Integer openSpots;
    private Integer reputationScore;
    private Long captainId;
    private String captainName;
    private List<TeamMemberResponse> members;
    private LocalDateTime createdAt;
}
