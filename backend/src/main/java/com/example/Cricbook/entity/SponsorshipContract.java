package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsorship_contracts", indexes = {
        @Index(name = "idx_sponsorship_contract_status", columnList = "status"),
        @Index(name = "idx_sponsorship_contract_brand", columnList = "brand_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorshipContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandPartner brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private SponsorshipProposal proposal;

    @Column(name = "contract_number", nullable = false, unique = true)
    private String contractNumber;

    @Column(name = "contract_type", nullable = false, length = 60)
    @Builder.Default
    private String contractType = "SPONSORSHIP";

    @Column(name = "contract_value", nullable = false)
    @Builder.Default
    private BigDecimal value = BigDecimal.ZERO;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
