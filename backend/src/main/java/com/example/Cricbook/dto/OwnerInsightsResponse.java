package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerInsightsResponse {
    private RevenueIntelligence revenue;
    private OccupancyIntelligence occupancy;
    private List<PeakHourCell> heatmap;
    private List<CustomerInsight> customers;
    private List<PricingSuggestion> pricingSuggestions;
    private List<BusinessPattern> bookingPatterns;
    private ProfitSummary profit;
    private ForecastInsight forecast;
    private List<PromotionSuggestion> promotions;
    private CommunityInsight community;
    private BusinessCoachResponse coach;
    private List<OwnerExpenseResponse> recentExpenses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueIntelligence {
        private BigDecimal todayRevenue;
        private BigDecimal weekRevenue;
        private BigDecimal lastWeekRevenue;
        private BigDecimal monthRevenue;
        private BigDecimal averageBookingValue;
        private Double weeklyGrowthPercent;
        private List<AnalyticsPoint> dailyTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupancyIntelligence {
        private Double occupancyPercent;
        private Long bookedSlots;
        private Long availableSlotCapacity;
        private Long unusedSlotCapacity;
        private Double peakUtilizationPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakHourCell {
        private String dayOfWeek;
        private Integer hour;
        private String hourLabel;
        private Long bookingCount;
        private Double utilizationPercent;
        private String intensity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInsight {
        private Long customerId;
        private String customerName;
        private Long bookings;
        private BigDecimal lifetimeValue;
        private LocalDate lastVisit;
        private String segment;
        private Integer trustScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingSuggestion {
        private Long turfId;
        private String turfName;
        private Long slotId;
        private BigDecimal currentPrice;
        private BigDecimal recommendedPrice;
        private String reason;
        private Integer confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessPattern {
        private String label;
        private String metric;
        private String insight;
        private String direction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitSummary {
        private BigDecimal revenue;
        private BigDecimal expenses;
        private BigDecimal profit;
        private Double marginPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastInsight {
        private String period;
        private BigDecimal expectedRevenue;
        private Double expectedOccupancy;
        private String demandLevel;
        private String insight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionSuggestion {
        private String title;
        private String description;
        private Integer suggestedDiscountPercent;
        private String expectedImpact;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommunityInsight {
        private Long repeatCustomers;
        private Long frequentCaptains;
        private Long activeTeams;
        private String communityScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessCoachRequest {
        private String question;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessCoachResponse {
        private String answer;
        private List<String> recommendedActions;
        private Integer confidence;
    }
}
