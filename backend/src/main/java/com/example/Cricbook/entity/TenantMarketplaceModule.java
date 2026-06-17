package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_marketplace_modules", indexes = {
        @Index(name = "idx_tenant_module_code", columnList = "module_code", unique = true),
        @Index(name = "idx_tenant_module_status", columnList = "status, category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantMarketplaceModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_code", nullable = false, unique = true)
    private String moduleCode;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private String category = "CORE";

    @Builder.Default
    @Column(nullable = false)
    private String pricingModel = "FREE";

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "INR";

    @Builder.Default
    @Column(nullable = false)
    private String certificationStatus = "CERTIFIED";

    @Builder.Default
    @Column(nullable = false)
    private String status = "PUBLISHED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
