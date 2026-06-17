package com.example.Cricbook.dto;

import com.example.Cricbook.enums.AutoWaitlistStatus;
import com.example.Cricbook.enums.RecurringBookingFrequency;
import com.example.Cricbook.enums.RecurringBookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class RebookDtos {
    private RebookDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RebookRequest {
        private Long bookingId;
        private LocalDate targetDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FrequentBookingResponse {
        private Long sourceBookingId;
        private Long turfId;
        private String turfName;
        private String city;
        private Long slotId;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal amount;
        private Integer timesBooked;
        private LocalDate lastBookedDate;
        private LocalDate nextSuggestedDate;
        private String recommendationReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingPredictionResponse {
        private Long turfId;
        private String turfName;
        private Long slotId;
        private LocalDate predictedDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private Double confidence;
        private String reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecurringBookingRequest {
        private Long turfId;
        private Long slotId;
        private Long teamId;
        private RecurringBookingFrequency frequency;
        private DayOfWeek preferredDay;
        private LocalDate nextBookingDate;
        private Boolean autoConfirm;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecurringBookingResponse {
        private Long id;
        private Long turfId;
        private String turfName;
        private Long slotId;
        private LocalTime startTime;
        private LocalTime endTime;
        private Long teamId;
        private RecurringBookingFrequency frequency;
        private DayOfWeek preferredDay;
        private LocalDate nextBookingDate;
        private Boolean autoConfirm;
        private RecurringBookingStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingTemplateRequest {
        private String templateName;
        private Long turfId;
        private Long teamId;
        private DayOfWeek preferredDay;
        private LocalTime preferredTime;
        private Integer durationMinutes;
        private Integer players;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingTemplateResponse {
        private Long id;
        private String templateName;
        private Long turfId;
        private String turfName;
        private Long teamId;
        private DayOfWeek preferredDay;
        private LocalTime preferredTime;
        private Integer durationMinutes;
        private Integer players;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AutoWaitlistRequest {
        private Long slotId;
        private LocalDate preferredDate;
        private Boolean autoBookEnabled;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AutoWaitlistResponse {
        private Long id;
        private Long slotId;
        private String turfName;
        private LocalDate preferredDate;
        private Boolean autoBookEnabled;
        private AutoWaitlistStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingIntelligenceResponse {
        private long frequentBookings;
        private long recurringBookings;
        private long templates;
        private long autoWaitlists;
        private long conflicts;
        private double rebookReadiness;
        private List<FrequentBookingResponse> topFrequentBookings;
        private List<BookingPredictionResponse> predictions;
    }
}
