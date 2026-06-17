package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_auctions", indexes = {
        @Index(name = "idx_federation_auction_body", columnList = "federationId"),
        @Index(name = "idx_federation_auction_player", columnList = "playerRegistrationId"),
        @Index(name = "idx_federation_auction_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationAuction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private Long playerRegistrationId;

    @Column(nullable = false)
    private String auctionName;

    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentBid = BigDecimal.ZERO;

    private Long highestBidFranchiseId;

    @Builder.Default
    private Integer bidCount = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PUBLISHED";

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
