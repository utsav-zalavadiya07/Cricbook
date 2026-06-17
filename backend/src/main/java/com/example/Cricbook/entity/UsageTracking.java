package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usage_tracking", indexes = {
        @Index(name = "idx_usage_user_feature", columnList = "user_id, feature, period_month")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature", nullable = false)
    private String feature;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount;

    @Column(name = "period_month", nullable = false) // YYYY-MM
    private String periodMonth;
}
