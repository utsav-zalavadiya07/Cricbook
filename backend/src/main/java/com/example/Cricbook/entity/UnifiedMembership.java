package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "unified_memberships", indexes = {
        @Index(name = "idx_unified_membership_identity", columnList = "identity_id"),
        @Index(name = "idx_unified_membership_status", columnList = "status"),
        @Index(name = "idx_unified_membership_tier", columnList = "tier")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_id", nullable = false)
    private Long identityId;

    @Column(nullable = false)
    private String tier;

    @Builder.Default
    @Column(nullable = false)
    private String billingCycle = "MONTHLY";

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean autoRenew = false;

    @Column(columnDefinition = "TEXT")
    private String benefitsSummary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
