package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartRecommendationResponse {
    private List<TurfResponse> bestTurfs;
    private List<String> bestTimings;
    private List<AdminUserResponse> playerSuggestions;
    private BigDecimal suggestedPrice;
    private String pricingReason;
}
