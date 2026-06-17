package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_video_analysis", indexes = {
        @Index(name = "idx_training_video_player", columnList = "playerEmail"),
        @Index(name = "idx_training_video_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingVideoAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long enrollmentId;

    private String playerEmail;

    private String category;

    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String coachComments;

    @Column(columnDefinition = "TEXT")
    private String improvementAreas;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
