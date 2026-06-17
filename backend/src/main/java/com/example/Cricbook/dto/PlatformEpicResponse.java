package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformEpicResponse {
    private String epicId;
    private String name;
    private String objective;
    private String releaseStage;
    private String defaultTheme;
    private List<String> sharedDomains;
    private List<PlatformEpicSurfaceResponse> surfaces;
    private List<PlatformEpicChecklistResponse> readiness;
    private List<String> notificationEvents;
    private List<String> analyticsEvents;
}
