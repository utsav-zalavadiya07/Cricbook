package com.example.Cricbook.entity;

import com.example.Cricbook.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column
    private NotificationChannel channel;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "deep_link")
    private String deepLink;

    @Column(nullable = false)
    @Builder.Default
    private String category = "GENERAL";

    @Column(nullable = false)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SENT, FAILED, READ, ARCHIVED

    @Column(name = "retry_count")
    private int retryCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}
