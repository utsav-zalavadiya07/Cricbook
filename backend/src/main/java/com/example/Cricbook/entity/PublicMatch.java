package com.example.Cricbook.entity;

import com.example.Cricbook.enums.MatchFormat;
import com.example.Cricbook.enums.MatchVisibility;
import com.example.Cricbook.enums.PublicMatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "public_matches", indexes = {
        @Index(name = "idx_public_match_city", columnList = "city"),
        @Index(name = "idx_public_match_date", columnList = "match_date"),
        @Index(name = "idx_public_match_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turf_id")
    private Turf turf;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String location;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_format", nullable = false)
    private MatchFormat format;

    @Column(name = "overs_count")
    private Integer overs;

    @Column(name = "entry_fee")
    private Double entryFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "result_margin")
    private String resultMargin;

    @Column(name = "challenge_message", columnDefinition = "TEXT")
    private String challengeMessage;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicMatchStatus status;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
