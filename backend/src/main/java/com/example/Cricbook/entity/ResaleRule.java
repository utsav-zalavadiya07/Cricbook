package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resale_rules", indexes = {
        @Index(name = "idx_resale_rules_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResaleRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "allow_resale", nullable = false)
    private Boolean allowResale;

    @Column(name = "auto_approve_transfer", nullable = false)
    private Boolean autoApproveTransfer;

    @Column(name = "restrict_low_trust_users", nullable = false)
    private Boolean restrictLowTrustUsers;

    @Column(name = "max_markup_percent", nullable = false)
    private Integer maxMarkupPercent;

    @Column(name = "min_hours_before_start", nullable = false)
    private Integer minHoursBeforeStart;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
