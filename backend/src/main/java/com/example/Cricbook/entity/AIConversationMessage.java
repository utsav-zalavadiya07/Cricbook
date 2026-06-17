package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_conversation_messages", indexes = {
        @Index(name = "idx_ai_conversation", columnList = "conversation_id, created_at"),
        @Index(name = "idx_ai_conversation_user", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_conversation_intent", columnList = "intent")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id", nullable = false, length = 80)
    private String conversationId;

    @Column(nullable = false, length = 64)
    private String intent;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "assistant_reply", nullable = false, columnDefinition = "TEXT")
    private String assistantReply;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "safety_flags", length = 500)
    private String safetyFlags;

    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
