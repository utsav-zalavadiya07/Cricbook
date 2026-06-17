package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_custom_domains", indexes = {
        @Index(name = "idx_tenant_custom_domain", columnList = "domain", unique = true),
        @Index(name = "idx_tenant_custom_domain_tenant", columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantCustomDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, unique = true)
    private String domain;

    @Column(nullable = false)
    private String verificationToken;

    @Builder.Default
    @Column(nullable = false)
    private String routingMode = "CNAME";

    @Builder.Default
    @Column(nullable = false)
    private String status = "PENDING_VERIFICATION";

    private LocalDateTime verifiedAt;

    private LocalDateTime activatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
