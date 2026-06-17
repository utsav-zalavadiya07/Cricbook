package com.example.Cricbook.dto;

import com.example.Cricbook.enums.RewardType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTournamentRewardRequest {
    private RewardType rewardType;
    private String title;
    private String description;
    private Integer position;
    private BigDecimal cashAmount;
    private Long winnerTeamId;
}
