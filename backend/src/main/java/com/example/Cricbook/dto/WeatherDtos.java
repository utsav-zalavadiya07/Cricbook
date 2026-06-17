package com.example.Cricbook.dto;

import com.example.Cricbook.enums.TurfEnvironmentType;
import com.example.Cricbook.enums.WeatherRefundType;
import com.example.Cricbook.enums.WeatherRiskLevel;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class WeatherDtos {
    private WeatherDtos() {
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ForecastResponse {
        private String city;
        private LocalDate date;
        private Integer hour;
        private Double temperature;
        private Double rainProbability;
        private Double humidity;
        private Double windSpeed;
        private Double uvIndex;
        private String summary;
        private WeatherRiskLevel riskLevel;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlayabilityResponse {
        private Long turfId;
        private String turfName;
        private Long slotId;
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer score;
        private WeatherRiskLevel riskLevel;
        private Double rainProbability;
        private Double temperature;
        private String reason;
        private String recommendation;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WeatherPolicyRequest {
        private Boolean rainRefundAllowed;
        private Boolean flexibleReschedule;
        private Integer rainThreshold;
        private Integer heatThresholdCelsius;
        private WeatherRefundType refundType;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WeatherPolicyResponse {
        private Boolean rainRefundAllowed;
        private Boolean flexibleReschedule;
        private Integer rainThreshold;
        private Integer heatThresholdCelsius;
        private WeatherRefundType refundType;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TurfEnvironmentRequest {
        private Long turfId;
        private TurfEnvironmentType type;
        private Boolean rainProtection;
        private Boolean heatProtection;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TurfEnvironmentResponse {
        private Long turfId;
        private String turfName;
        private TurfEnvironmentType type;
        private Boolean rainProtection;
        private Boolean heatProtection;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WeatherRescheduleResponse {
        private Long id;
        private Long bookingId;
        private Long suggestedSlotId;
        private LocalDate suggestedDate;
        private String status;
        private String reason;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WeatherRefundResponse {
        private Long id;
        private Long bookingId;
        private WeatherRefundType refundType;
        private BigDecimal amount;
        private String status;
        private String reason;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WeatherDashboardResponse {
        private String city;
        private ForecastResponse current;
        private List<ForecastResponse> forecast;
        private List<PlayabilityResponse> recommendations;
        private long highRiskBookings;
        private long weatherRefunds;
        private String insight;
    }
}
