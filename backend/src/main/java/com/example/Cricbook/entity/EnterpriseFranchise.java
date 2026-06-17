package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "enterprise_franchises", indexes = {
        @Index(name = "idx_enterprise_franchise_business", columnList = "business_id"),
        @Index(name = "idx_enterprise_franchise_center", columnList = "center_id"),
        @Index(name = "idx_enterprise_franchise_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseFranchise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private EnterpriseBusiness business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private EnterpriseCenter center;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(name = "revenue_share_percent", nullable = false)
    private BigDecimal revenueSharePercent;

    @Column(name = "agreement_start")
    private LocalDate agreementStart;

    @Column(name = "agreement_end")
    private LocalDate agreementEnd;

    @Column(name = "compliance_status", nullable = false)
    private String complianceStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
