package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TeamFinanceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TeamFinanceResponse {
    private Long id;
    private Long teamId;
    private Long userId;
    private String userName;
    private TeamFinanceType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;
}
