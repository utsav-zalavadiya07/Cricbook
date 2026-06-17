package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_feature_toggles", indexes = {
        @Index(name = "idx_tenant_feature_lookup", columnList = "tenant_id, feature_key", unique = true),
        @Index(name = "idx_tenant_feature_enabled", columnList = "feature_key, enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeatureToggle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "feature_key", nullable = false)
    private String featureKey;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = false;

    @Builder.Default
    @Column(nullable = false)
    private String rolloutStrategy = "TENANT_OVERRIDE";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
