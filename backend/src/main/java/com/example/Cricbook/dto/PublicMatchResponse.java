package com.example.Cricbook.dto;

import com.example.Cricbook.enums.MatchFormat;
import com.example.Cricbook.enums.MatchVisibility;
import com.example.Cricbook.enums.PublicMatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicMatchResponse {
    private Long id;
    private Long creatorId;
    private String creatorName;
    private Long turfId;
    private String turfName;
    private String title;
    private String sport;
    private String city;
    private String location;
    private LocalDate matchDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxPlayers;
    private Integer joinedPlayers;
    private MatchVisibility visibility;
    private MatchFormat format;
    private Integer overs;
    private Double entryFee;
    private Long homeTeamId;
    private String homeTeamName;
    private Long awayTeamId;
    private String awayTeamName;
    private Long winnerTeamId;
    private String winnerTeamName;
    private Integer homeScore;
    private Integer awayScore;
    private String resultMargin;
    private String challengeMessage;
    private String notes;
    private String resultSummary;
    private PublicMatchStatus status;
    private List<TeamMemberResponse> participants;
    private LocalDateTime acceptedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
