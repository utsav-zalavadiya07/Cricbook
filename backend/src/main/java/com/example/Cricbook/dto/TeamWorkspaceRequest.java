package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class TeamWorkspaceRequest {
    private String visibility;
    private String teamType;
    private String bannerUrl;
    private String goals;
}
