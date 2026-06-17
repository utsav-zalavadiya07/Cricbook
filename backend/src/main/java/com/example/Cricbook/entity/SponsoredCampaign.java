package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsored_campaigns", indexes = {
        @Index(name = "idx_sponsored_campaign_status", columnList = "status"),
        @Index(name = "idx_sponsored_campaign_type", columnList = "campaign_type"),
        @Index(name = "idx_sponsored_campaign_brand", columnList = "brand_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private BrandPartner brand;

    @Column(nullable = false)
    private String name;

    @Column(name = "campaign_type", nullable = false, length = 60)
    @Builder.Default
    private String campaignType = "BANNER_AD";

    @Column(name = "placement", nullable = false, length = 80)
    @Builder.Default
    private String placement = "HOMEPAGE";

    @Column(name = "target_audience", length = 120)
    @Builder.Default
    private String targetAudience = "ALL_USERS";

    private String city;

    private String creativeUrl;

    private String destinationUrl;

    @Column(name = "daily_budget", nullable = false)
    @Builder.Default
    private BigDecimal dailyBudget = BigDecimal.ZERO;

    @Column(name = "lifetime_budget", nullable = false)
    @Builder.Default
    private BigDecimal lifetimeBudget = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal spend = BigDecimal.ZERO;

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
