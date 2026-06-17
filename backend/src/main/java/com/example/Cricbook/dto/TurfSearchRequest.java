package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurfSearchRequest {
    private String query;
    private String city;
    private Double latitude;
    private Double longitude;
    private Double radius; // in km

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private String boxType;
    private String pitchType;
    private String sport;
    private String sportCode;

    private List<String> amenities;

    private LocalDate date;
    private LocalTime startTime;

    private String sortBy; // PRICE_ASC, PRICE_DESC, DISTANCE, RATING, MOST_BOOKED
    private Double rating;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
