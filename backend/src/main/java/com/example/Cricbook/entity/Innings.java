package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "innings", indexes = {
        @Index(name = "idx_innings_live_score", columnList = "live_score_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Innings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_score_id", nullable = false)
    private LiveMatchScore liveScore;

    @Column(name = "innings_number", nullable = false)
    private Integer inningsNumber;

    @Column(name = "batting_team_name", nullable = false)
    private String battingTeamName;

    @Column(nullable = false)
    private Integer runs;

    @Column(nullable = false)
    private Integer wickets;

    @Column(name = "balls_bowled", nullable = false)
    private Integer ballsBowled;

    @Column(nullable = false)
    private Integer extras;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
