package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_trust_score", indexes = {
        @Index(name = "idx_player_trust_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerTrustScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "attendance_score", nullable = false)
    private Integer attendanceScore;

    @Column(name = "punctuality_score", nullable = false)
    private Integer punctualityScore;

    @Column(name = "cancellation_score", nullable = false)
    private Integer cancellationScore;

    @Column(name = "report_score", nullable = false)
    private Integer reportScore;

    @Column(name = "payment_score", nullable = false)
    private Integer paymentScore;

    @Column(name = "match_completion_score", nullable = false)
    private Integer matchCompletionScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
