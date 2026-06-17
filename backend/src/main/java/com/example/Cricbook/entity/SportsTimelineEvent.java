package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_timeline_events", indexes = {
        @Index(name = "idx_sports_timeline_identity", columnList = "identity_id, occurred_at"),
        @Index(name = "idx_sports_timeline_module", columnList = "source_module")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_id", nullable = false)
    private Long identityId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String sourceModule;

    @Column(nullable = false)
    private String title;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder.Default
    @Column(nullable = false)
    private String visibility = "PRIVATE";

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
