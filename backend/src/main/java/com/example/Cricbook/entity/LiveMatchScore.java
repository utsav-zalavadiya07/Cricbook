package com.example.Cricbook.entity;

import com.example.Cricbook.enums.LiveMatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_match_scores", indexes = {
        @Index(name = "idx_live_score_status", columnList = "status"),
        @Index(name = "idx_live_score_fixture", columnList = "fixture_id"),
        @Index(name = "idx_live_score_public_match", columnList = "public_match_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveMatchScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id")
    private TournamentFixture fixture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_match_id")
    private PublicMatch publicMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scorer_id", nullable = false)
    private User scorer;

    @Column(nullable = false)
    private String title;

    @Column(name = "team_a_name", nullable = false)
    private String teamAName;

    @Column(name = "team_b_name", nullable = false)
    private String teamBName;

    @Column(name = "batting_team", nullable = false)
    private String battingTeam;

    @Column(name = "bowling_team", nullable = false)
    private String bowlingTeam;

    @Column(nullable = false)
    private Integer innings;

    @Column(nullable = false)
    private Integer runs;

    @Column(nullable = false)
    private Integer wickets;

    @Column(name = "balls_bowled", nullable = false)
    private Integer ballsBowled;

    @Column(nullable = false)
    private Integer extras;

    @Column(name = "target_runs")
    private Integer targetRuns;

    @Column(name = "max_overs", nullable = false)
    private Integer maxOvers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LiveMatchStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Column(name = "is_tie", nullable = false)
    @Builder.Default
    private Boolean isTie = false;

    @Column(name = "is_super_over", nullable = false)
    @Builder.Default
    private Boolean isSuperOver = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
