package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsorship_opportunities", indexes = {
        @Index(name = "idx_sponsor_opportunity_status", columnList = "status"),
        @Index(name = "idx_sponsor_opportunity_city", columnList = "city"),
        @Index(name = "idx_sponsor_opportunity_type", columnList = "opportunity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorshipOpportunity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "opportunity_type", nullable = false, length = 60)
    @Builder.Default
    private String opportunityType = "TOURNAMENT";

    @Column(length = 1500)
    private String description;

    private String city;

    @Column(name = "sport_category", length = 60)
    @Builder.Default
    private String sportCategory = "CRICKET";

    @Column(name = "audience_size")
    @Builder.Default
    private Integer audienceSize = 0;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String benefits;

    @Column(name = "owner_reference")
    private String ownerReference;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
