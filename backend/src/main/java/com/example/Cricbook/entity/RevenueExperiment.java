package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "revenue_experiments", indexes = {
        @Index(name = "idx_experiment_variant", columnList = "experiment_name, variant")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueExperiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_name", nullable = false)
    private String experimentName;

    @Column(nullable = false)
    private String variant; // A or B

    @Column(nullable = false)
    private Integer conversions;

    @Column(nullable = false)
    private Integer impressions;

    @Column(name = "conversion_rate", nullable = false)
    private BigDecimal conversionRate;
}
