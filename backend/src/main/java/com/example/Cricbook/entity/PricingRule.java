package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules", indexes = {
        @Index(name = "idx_pricing_rule_turf", columnList = "turf_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turf_id", nullable = false)
    private Long turfId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName; // PEAK_HOUR, WEEKEND, OFF_PEAK

    @Column(name = "adjustment_percent", nullable = false)
    private BigDecimal adjustmentPercent; // e.g. 15.00 for +15%, -10.00 for -10%

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
