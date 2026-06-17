package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_app_builds", indexes = {
        @Index(name = "idx_tenant_app_build_tenant", columnList = "tenant_id, status"),
        @Index(name = "idx_tenant_app_build_platform", columnList = "platform, app_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantAppBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Builder.Default
    @Column(nullable = false)
    private String appType = "WHITE_LABEL";

    @Builder.Default
    @Column(nullable = false)
    private String platform = "WEB";

    @Builder.Default
    @Column(nullable = false)
    private String version = "1.0.0";

    private String artifactUrl;

    @Builder.Default
    @Column(nullable = false)
    private String status = "QUEUED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
