package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_assessments", indexes = {
        @Index(name = "idx_player_assessment_enrollment", columnList = "enrollmentId"),
        @Index(name = "idx_player_assessment_player", columnList = "playerEmail")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long enrollmentId;

    private Long coachId;

    private String playerName;

    private String playerEmail;

    private String sport;

    private Integer battingScore;

    private Integer bowlingScore;

    private Integer fieldingScore;

    private Integer fitnessScore;

    private Integer disciplineScore;

    private Integer overallScore;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
