package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "holiday_pricing_rules", indexes = {
        @Index(name = "idx_holiday_rule_turf", columnList = "turf_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayPricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turf_id", nullable = false, unique = true)
    private Long turfId;

    @Column(name = "auto_apply", nullable = false)
    @Builder.Default
    private Boolean autoApply = false;

    @Column(name = "pricing_mode", nullable = false, length = 50)
    @Builder.Default
    private String pricingMode = "PERCENTAGE"; // PERCENTAGE, FIXED, MULTIPLIER

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal value = BigDecimal.ZERO;

    @Column(name = "apply_to", nullable = false, length = 50)
    @Builder.Default
    private String applyTo = "ALL"; // ALL, SELECTED

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "holiday_rule_exclusions", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "holiday_name")
    @Builder.Default
    private Set<String> excludedHolidays = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "holiday_rule_slots", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "slot_id")
    @Builder.Default
    private Set<Long> slotIds = new HashSet<>();
}
