package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamVisibility;
import lombok.Data;

@Data
public class CreateTeamRequest {
    private String name;
    private String sport;
    private String city;
    private String description;
    private String logoUrl;
    private Integer maxMembers;
    private TeamVisibility visibility;
}
