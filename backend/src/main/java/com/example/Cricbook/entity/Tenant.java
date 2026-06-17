package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_domain", columnList = "domain"),
        @Index(name = "idx_tenant_code", columnList = "tenant_code", unique = true),
        @Index(name = "idx_tenant_region_status", columnList = "country_code, region_code, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_code", unique = true, length = 48)
    private String tenantCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(columnDefinition = "TEXT")
    private String theme; // custom theme HSL or config parameters

    @Builder.Default
    @Column(nullable = false)
    private String tenantType = "STANDARD";

    @Builder.Default
    @Column(nullable = false)
    private String countryCode = "IN";

    @Builder.Default
    @Column(nullable = false)
    private String regionCode = "IN-GJ";

    @Builder.Default
    @Column(nullable = false)
    private String defaultLocale = "en";

    @Builder.Default
    @Column(nullable = false)
    private String currency = "INR";

    @Builder.Default
    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, ARCHIVED, TRIAL, EXPIRED

    @Builder.Default
    @Column(nullable = false)
    private String lifecycleStage = "TRIAL";

    @Builder.Default
    @Column(nullable = false)
    private Boolean whiteLabelEnabled = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
