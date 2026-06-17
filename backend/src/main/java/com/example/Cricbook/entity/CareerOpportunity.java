package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "career_opportunities", indexes = {
        @Index(name = "idx_career_opp_status", columnList = "status"),
        @Index(name = "idx_career_opp_type", columnList = "opportunityType"),
        @Index(name = "idx_career_opp_sport", columnList = "sport"),
        @Index(name = "idx_career_opp_location", columnList = "location")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String opportunityType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String organizationName;

    @Column(nullable = false)
    private String sport;

    private String category;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    private Integer capacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer applicantCount = 0;

    private LocalDate deadline;

    private LocalDateTime startsAt;

    private BigDecimal scholarshipAmount;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    private String createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
