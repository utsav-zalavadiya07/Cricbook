package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "trial_evaluations", indexes = {
        @Index(name = "idx_trial_eval_application", columnList = "application_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private CareerApplication application;

    @Column(nullable = false)
    private String evaluatorName;

    private Integer techniqueScore;

    private Integer fitnessScore;

    private Integer disciplineScore;

    private Integer awarenessScore;

    @Column(nullable = false)
    private Integer overallScore;

    @Column(nullable = false, length = 40)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
