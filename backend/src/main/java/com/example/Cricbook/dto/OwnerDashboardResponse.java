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
public class OwnerDashboardResponse {
    private Long todayBookings;
    private Long totalBookings;
    private BigDecimal todayRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal totalRevenue;
    private BigDecimal last14DaysRevenue;
    private Integer periodDays;
    private BigDecimal periodRevenue;
    private Double revenueGrowthPercent;
    private Double occupancyPercent;
    private Double cancellationPercent;
    private Long activeTurfs;
    private Long inactiveTurfs;
    private Long maintenanceTurfs;
    private TurfResponse popularTurf;
    private List<AnalyticsPoint> revenueSeries;
    private List<BookingResponse> upcomingBookings;
    private List<ReviewResponse> recentReviews;
}
