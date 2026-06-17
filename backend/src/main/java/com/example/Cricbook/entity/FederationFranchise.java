package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_franchises", indexes = {
        @Index(name = "idx_federation_franchise_body", columnList = "federationId"),
        @Index(name = "idx_federation_franchise_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationFranchise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private String name;

    private String ownerName;

    private String city;

    private String homeVenue;

    @Builder.Default
    private Integer titlesWon = 0;

    @Builder.Default
    private Integer rosterSize = 0;

    @Builder.Default
    private BigDecimal salaryCap = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal usedBudget = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String ownershipHistory;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
