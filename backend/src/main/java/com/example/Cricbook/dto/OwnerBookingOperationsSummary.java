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
public class OwnerBookingOperationsSummary {
    private Long totalBookings;
    private Long todayBookings;
    private Long upcomingBookings;
    private Long activeMatches;
    private Long pendingArrivals;
    private Long checkedInToday;
    private Long completedBookings;
    private Long cancelledBookings;
    private Long noShowBookings;
    private BigDecimal todayRevenue;
    private Double attendanceRate;
    private Double noShowRate;
    private List<OwnerBookingTimelineResponse> recentActivity;
}
