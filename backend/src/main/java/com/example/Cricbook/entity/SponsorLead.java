package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsor_leads", indexes = {
        @Index(name = "idx_sponsor_lead_status", columnList = "status"),
        @Index(name = "idx_sponsor_lead_brand", columnList = "brand_id"),
        @Index(name = "idx_sponsor_lead_campaign", columnList = "campaign_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorLead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandPartner brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private SponsoredCampaign campaign;

    @Column(name = "lead_type", nullable = false, length = 60)
    @Builder.Default
    private String leadType = "PLAYER";

    @Column(nullable = false)
    private String name;

    private String email;

    private String phone;

    private String city;

    @Column(length = 1200)
    private String notes;

    @Column(name = "estimated_value", nullable = false)
    @Builder.Default
    private BigDecimal estimatedValue = BigDecimal.ZERO;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "NEW";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
