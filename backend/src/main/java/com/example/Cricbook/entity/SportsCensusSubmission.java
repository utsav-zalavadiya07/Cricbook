package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_census_submissions", indexes = {
        @Index(name = "idx_sports_census_cycle", columnList = "cycle_code"),
        @Index(name = "idx_sports_census_geo", columnList = "state_name, district_name"),
        @Index(name = "idx_sports_census_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsCensusSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cycleCode;

    @Column(nullable = false)
    private String jurisdictionLevel;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = false)
    private String districtName;

    private String submittedBy;

    @Builder.Default
    private Integer playerCount = 0;

    @Builder.Default
    private Integer coachCount = 0;

    @Builder.Default
    private Integer academyCount = 0;

    @Builder.Default
    private Integer facilityCount = 0;

    @Builder.Default
    private Integer schoolCount = 0;

    @Builder.Default
    private Integer universityCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private String status = "SUBMITTED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
