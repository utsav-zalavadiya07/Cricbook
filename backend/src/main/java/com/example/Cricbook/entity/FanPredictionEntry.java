package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fan_prediction_entries", indexes = {
        @Index(name = "idx_fan_prediction_entry_game", columnList = "game_id, fan_profile_id"),
        @Index(name = "idx_fan_prediction_entry_status", columnList = "result_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanPredictionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "fan_profile_id", nullable = false)
    private Long fanProfileId;

    @Column(nullable = false)
    private String predictionValue;

    @Builder.Default
    @Column(name = "result_status", nullable = false)
    private String resultStatus = "PENDING";

    @Builder.Default
    @Column(nullable = false)
    private Integer pointsAwarded = 0;

    @Column(columnDefinition = "TEXT")
    private String settlementNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
