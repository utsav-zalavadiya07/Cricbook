package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_match_stats", indexes = {
        @Index(name = "idx_player_match_stats_user", columnList = "user_id"),
        @Index(name = "idx_player_match_stats_live_score", columnList = "live_score_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_score_id", nullable = false)
    private LiveMatchScore liveScore;

    @Column(name = "runs_scored", nullable = false)
    @Builder.Default
    private Integer runsScored = 0;

    @Column(name = "balls_faced", nullable = false)
    @Builder.Default
    private Integer ballsFaced = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer fours = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer sixes = 0;

    @Column(name = "wickets_taken", nullable = false)
    @Builder.Default
    private Integer wicketsTaken = 0;

    @Column(name = "overs_bowled", nullable = false)
    @Builder.Default
    private Double oversBowled = 0.0;

    @Column(name = "runs_conceded", nullable = false)
    @Builder.Default
    private Integer runsConceded = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer catches = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer runouts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
