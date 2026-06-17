package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tournament_standings", uniqueConstraints = {
        @UniqueConstraint(name = "unique_tournament_standing_team", columnNames = {"tournament_id", "team_id"})
}, indexes = {
        @Index(name = "idx_standing_tournament", columnList = "tournament_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private Integer played;

    @Column(nullable = false)
    private Integer wins;

    @Column(nullable = false)
    private Integer losses;

    @Builder.Default
    @Column(nullable = false)
    private Integer draws = 0;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "runs_for", nullable = false)
    private Integer runsFor;

    @Column(name = "runs_against", nullable = false)
    private Integer runsAgainst;
}
