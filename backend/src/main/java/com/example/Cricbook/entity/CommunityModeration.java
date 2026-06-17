package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_moderation", indexes = @Index(name = "idx_community_moderation_content", columnList = "content_type,content_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityModeration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_type", nullable = false, length = 60)
    private String contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "moderator_id", length = 160)
    private String moderatorId;

    @Column(nullable = false, length = 800)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
