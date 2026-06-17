package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_histories", indexes = {
        @Index(name = "idx_pricing_history_turf", columnList = "turf_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turf_id", nullable = false)
    private Long turfId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "original_price", nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "dynamic_price", nullable = false)
    private BigDecimal dynamicPrice;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
