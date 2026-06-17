package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction_games", indexes = {
        @Index(name = "idx_prediction_game_code", columnList = "game_code", unique = true),
        @Index(name = "idx_prediction_game_status_deadline", columnList = "status, deadline_at"),
        @Index(name = "idx_prediction_game_target", columnList = "target_type, target_ref_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_code", nullable = false, unique = true, length = 48)
    private String gameCode;

    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private String sport = "CRICKET";

    private String targetType;

    @Column(name = "target_ref_id")
    private Long targetRefId;

    @Builder.Default
    @Column(nullable = false)
    private String predictionType = "MATCH_WINNER";

    @Builder.Default
    @Column(nullable = false)
    private Integer pointsForCorrect = 100;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @Column(columnDefinition = "TEXT")
    private String settlementRule;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
