package com.example.Cricbook.entity;

import com.example.Cricbook.enums.MembershipPlanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_plans", indexes = {
        @Index(name = "idx_membership_plan_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipPlanType type;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "priority_slots", nullable = false)
    private Boolean prioritySlots;

    @Column(name = "featured_listing", nullable = false)
    private Boolean featuredListing;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
