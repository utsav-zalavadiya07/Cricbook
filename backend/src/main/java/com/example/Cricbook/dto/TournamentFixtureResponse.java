package com.example.Cricbook.dto;

import com.example.Cricbook.enums.FixtureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentFixtureResponse {
    private Long id;
    private Long tournamentId;
    private Long teamAId;
    private String teamAName;
    private Long teamBId;
    private String teamBName;
    private Long winnerTeamId;
    private String winnerTeamName;
    private Integer round;
    private LocalDate matchDate;
    private Long turfId;
    private String turfName;
    private Long slotId;
    private LocalTime startTime;
    private String scheduleNote;
    private Integer teamAScore;
    private Integer teamBScore;
    private String stageLabel;
    private Boolean knockoutStage;
    private FixtureStatus status;
}
