package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_analytics", indexes = {
        @Index(name = "idx_match_analytics_match", columnList = "match_id"),
        @Index(name = "idx_match_analytics_metric", columnList = "metric")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(nullable = false)
    private String metric;

    @Column(name = "metric_value", columnDefinition = "TEXT", nullable = false)
    private String value;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
