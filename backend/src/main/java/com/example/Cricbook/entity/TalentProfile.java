package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "talent_profiles", indexes = {
        @Index(name = "idx_talent_status", columnList = "status"),
        @Index(name = "idx_talent_city", columnList = "city"),
        @Index(name = "idx_talent_sport", columnList = "sport")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long academyId;

    private Long enrollmentId;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String playerEmail;

    private Integer age;

    private String ageGroup;

    private String sport;

    private String city;

    private Integer assessmentScore;

    private Integer tournamentImpactScore;

    private Integer talentScore;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @Column(columnDefinition = "TEXT")
    private String highlights;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
