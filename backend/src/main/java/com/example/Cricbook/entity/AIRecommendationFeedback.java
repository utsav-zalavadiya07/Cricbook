package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_recommendation_feedback", indexes = {
        @Index(name = "idx_ai_feedback_user", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_feedback_target", columnList = "target_type, target_id"),
        @Index(name = "idx_ai_feedback_type", columnList = "feedback_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "feedback_type", nullable = false, length = 32)
    private String feedbackType;

    @Column(length = 500)
    private String reason;

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
