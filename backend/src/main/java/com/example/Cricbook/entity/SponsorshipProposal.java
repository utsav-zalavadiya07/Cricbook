package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsorship_proposals", indexes = {
        @Index(name = "idx_sponsor_proposal_status", columnList = "status"),
        @Index(name = "idx_sponsor_proposal_brand", columnList = "brand_id"),
        @Index(name = "idx_sponsor_proposal_opportunity", columnList = "opportunity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorshipProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandPartner brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private SponsorshipOpportunity opportunity;

    @Column(length = 1500)
    private String message;

    @Column(name = "proposed_amount", nullable = false)
    @Builder.Default
    private BigDecimal proposedAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String deliverables;

    private String documentUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(length = 1000)
    private String decisionNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
