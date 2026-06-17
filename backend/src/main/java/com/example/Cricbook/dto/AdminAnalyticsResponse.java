package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalOwners;
    private Long totalTurfs;
    private Long activeTurfs;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private Long reviewCount;
    private Long flaggedRiskCount;
}
