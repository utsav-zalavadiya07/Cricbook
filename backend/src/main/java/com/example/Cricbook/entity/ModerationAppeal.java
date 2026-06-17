package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_appeals", indexes = @Index(name = "idx_moderation_appeal_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationAppeal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "case_type", nullable = false, length = 60)
    private String caseType;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reviewed_by", length = 160)
    private String reviewedBy;

    @Column(name = "decision_reason", length = 800)
    private String decisionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
