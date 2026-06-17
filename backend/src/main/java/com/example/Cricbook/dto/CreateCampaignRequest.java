package com.example.Cricbook.dto;

import com.example.Cricbook.enums.NotificationChannel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateCampaignRequest {
    private String name;
    private String title;
    private String message;
    private NotificationChannel channel;
    private String targetRole;
    private String deepLink;
    private LocalDateTime scheduledAt;
}
