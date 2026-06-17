package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "team_competition", indexes = {
        @Index(name = "idx_team_competition_season", columnList = "season_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamCompetition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    @Column(name = "competition_rank", nullable = false)
    @Builder.Default
    private Integer rank = 0;

    @Column(name = "season_name", nullable = false)
    @Builder.Default
    private String seasonName = "Current Season";

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
