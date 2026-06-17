package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TeamAnnouncementResponse {
    private Long id;
    private Long teamId;
    private String title;
    private String content;
    private Boolean pinned;
    private String createdByName;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
