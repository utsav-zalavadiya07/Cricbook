package com.example.Cricbook.dto;

import com.example.Cricbook.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResponse {
        private Long id;
        private String title;
        private String message;
        private String status;
        private String deliveryStatus;
        private boolean read;
        private boolean archived;
        private NotificationChannel channel;
        private String category;
        private String type;
        private String priority;
        private Long campaignId;
        private String deepLink;
        private int retryCount;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
        private LocalDateTime archivedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSummaryResponse {
        private long total;
        private long unread;
        private long read;
        private long archived;
        private long pending;
        private long failed;
        private Map<String, Long> byChannel;
        private Map<String, Long> byCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceResponse {
        private Long id;
        private NotificationChannel channel;
        private boolean enabled;
        private String frequency;
        private List<String> disabledCategories;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceUpdateRequest {
        private String channel;
        private Boolean enabled;
        private String frequency;
        private String category;
        private Boolean categoryEnabled;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastRequest {
        private String title;
        private String message;
        private List<String> channels;
        private String targetRole;
        private List<Long> targetUserIds;
        private String category;
        private String priority;
        private String deepLink;
        private Long campaignId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastResponse {
        private int recipientCount;
        private int queuedCount;
        private List<String> channels;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplatePreviewRequest {
        private String templateName;
        private String subject;
        private String body;
        private String channel;
        private Map<String, String> variables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplatePreviewResponse {
        private String subject;
        private String body;
        private NotificationChannel channel;
        private Map<String, String> variables;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationAnalyticsResponse {
        private long total;
        private long sent;
        private long pending;
        private long failed;
        private long read;
        private long archived;
        private long views;
        private long clicks;
        private long conversions;
        private double deliveryRate;
        private double openRate;
        private double clickRate;
        private Map<String, Long> byChannel;
        private Map<String, Long> byCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventCatalogResponse {
        private String event;
        private String category;
        private String deepLink;
        private List<String> recommendedChannels;
        private List<String> requiredPayload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkNotificationRequest {
        private List<Long> ids;
    }
}
