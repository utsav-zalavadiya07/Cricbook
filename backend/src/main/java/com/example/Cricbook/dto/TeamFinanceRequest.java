package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamFinanceType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeamFinanceRequest {
    private Long userId;
    private TeamFinanceType type;
    private BigDecimal amount;
    private String description;
}
