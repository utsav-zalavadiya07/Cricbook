package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_auction_bids", indexes = {
        @Index(name = "idx_federation_auction_bid_auction", columnList = "auctionId"),
        @Index(name = "idx_federation_auction_bid_franchise", columnList = "franchiseId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationAuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long franchiseId;

    @Column(nullable = false)
    private BigDecimal bidAmount;

    private String bidderName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
