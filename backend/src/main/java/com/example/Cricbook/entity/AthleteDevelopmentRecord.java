package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "athlete_development_records", indexes = {
        @Index(name = "idx_athlete_development_geo", columnList = "state_name, district_name"),
        @Index(name = "idx_athlete_development_level", columnList = "current_level"),
        @Index(name = "idx_athlete_development_sport", columnList = "sport")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AthleteDevelopmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sportsIdentityId;

    @Column(nullable = false)
    private String athleteName;

    private String schoolOrInstitution;

    @Column(nullable = false)
    private String sport;

    @Builder.Default
    private String ageGroup = "OPEN";

    @Column(nullable = false)
    private String districtName;

    @Column(nullable = false)
    private String stateName;

    @Builder.Default
    @Column(nullable = false)
    private String currentLevel = "SCHOOL";

    @Builder.Default
    @Column(nullable = false)
    private String selectionStatus = "TRACKING";

    @Column(columnDefinition = "TEXT")
    private String selectionHistory;

    @Builder.Default
    private Integer progressScore = 50;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
