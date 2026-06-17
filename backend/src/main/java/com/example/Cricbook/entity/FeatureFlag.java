package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_flags", indexes = @Index(name = "idx_feature_flag_key", columnList = "feature_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_key", nullable = false, unique = true)
    private String featureKey;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "rollout_percentage", nullable = false)
    @Builder.Default
    private Integer rolloutPercentage = 0;

    @Column(name = "target_audience", nullable = false)
    @Builder.Default
    private String targetAudience = "ALL";

    @Column(length = 800)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
