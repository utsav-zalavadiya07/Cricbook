package com.example.Cricbook.dto;

import com.example.Cricbook.enums.CampaignStatus;
import com.example.Cricbook.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private Long id;
    private String name;
    private String title;
    private String message;
    private NotificationChannel channel;
    private String targetRole;
    private String deepLink;
    private CampaignStatus status;
    private Integer sentCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}
