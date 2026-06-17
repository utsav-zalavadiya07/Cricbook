package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "owner_plan_limits", indexes = {
        @Index(name = "idx_owner_plan_limit_name", columnList = "plan_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerPlanLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_name", nullable = false, unique = true)
    private String planName;

    @Column(name = "max_turf_count", nullable = false)
    private Integer maxTurfCount;

    @Column(name = "max_promotions_count", nullable = false)
    private Integer maxPromotionsCount;
}
