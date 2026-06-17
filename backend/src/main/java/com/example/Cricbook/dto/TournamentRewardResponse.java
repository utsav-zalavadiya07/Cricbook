package com.example.Cricbook.dto;

import com.example.Cricbook.enums.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRewardResponse {
    private Long id;
    private Long tournamentId;
    private RewardType rewardType;
    private String title;
    private String description;
    private Integer position;
    private BigDecimal cashAmount;
    private Long winnerTeamId;
    private String winnerTeamName;
    private LocalDateTime createdAt;
}
