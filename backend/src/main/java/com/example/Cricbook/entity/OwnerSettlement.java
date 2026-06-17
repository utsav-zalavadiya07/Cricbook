package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "owner_settlements", indexes = {
        @Index(name = "idx_owner_settlement_owner", columnList = "owner_id"),
        @Index(name = "idx_owner_settlement_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "gross_amount", nullable = false)
    private BigDecimal grossAmount;

    @Column(nullable = false)
    private BigDecimal commission;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "reference", length = 120)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
