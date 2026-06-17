package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fan_marketplace_listings", indexes = {
        @Index(name = "idx_fan_marketplace_code", columnList = "listing_code", unique = true),
        @Index(name = "idx_fan_marketplace_status", columnList = "status"),
        @Index(name = "idx_fan_marketplace_owner", columnList = "owner_fan_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanMarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_code", nullable = false, unique = true, length = 48)
    private String listingCode;

    @Column(name = "owner_fan_profile_id", nullable = false)
    private Long ownerFanProfileId;

    @Column(name = "collectible_ownership_id", nullable = false)
    private Long collectibleOwnershipId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "INR";

    private Long buyerFanProfileId;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
