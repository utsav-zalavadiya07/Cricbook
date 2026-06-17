package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TrustRiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrustScoreResponse {
    private Long userId;
    private String playerName;
    private Integer score;
    private String tier;
    private Integer attendanceScore;
    private Integer punctualityScore;
    private Integer cancellationScore;
    private Integer reportScore;
    private Integer paymentScore;
    private Integer matchCompletionScore;
    private TrustRiskLevel riskLevel;
    private String riskReason;
    private List<String> badges;
    private List<String> recommendations;
    private LocalDateTime updatedAt;
}
