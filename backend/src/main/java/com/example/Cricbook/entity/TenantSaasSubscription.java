package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_saas_subscriptions", indexes = {
        @Index(name = "idx_tenant_saas_subscription_tenant", columnList = "tenant_id, status"),
        @Index(name = "idx_tenant_saas_subscription_plan", columnList = "plan_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSaasSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String planCode;

    @Builder.Default
    @Column(nullable = false)
    private String billingCycle = "MONTHLY";

    @Builder.Default
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "INR";

    @Builder.Default
    @Column(nullable = false)
    private Boolean autoRenew = true;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String entitlements;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
