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
public class TurfSearchResponse {
    private Long id;
    private String name;
    private String city;
    private String location;
    private BigDecimal pricePerHour;
    private BigDecimal startingSlotPrice;
    private BigDecimal maximumSlotPrice;
    private Double rating;
    private Integer totalReviews;
    private String boxType;
    private String pitchType;
    private String sport;
    private List<String> sports;
    private String status;
    private Double distance;
    private List<String> amenities;
    private List<String> images;
    private boolean available;
}
