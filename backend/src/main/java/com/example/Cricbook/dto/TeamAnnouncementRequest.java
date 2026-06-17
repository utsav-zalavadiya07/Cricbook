package com.example.Cricbook.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamAnnouncementRequest {
    private String title;
    private String content;
    private Boolean pinned;
    private LocalDateTime expiresAt;
}
