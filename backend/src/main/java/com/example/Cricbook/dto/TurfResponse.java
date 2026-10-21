package com.example.Cricbook.dto;

import com.example.Cricbook.enums.BoxType;
import com.example.Cricbook.enums.PitchType;
import com.example.Cricbook.enums.TurfStatus;
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
public class TurfResponse {
    private Long id;
    private String name;
    private String description;
    private String location;
    private String city;
    private String country;
    private String state;
    private String timezone;
    private String holidaySource;
    private Integer capacity;
    private Double latitude;
    private Double longitude;
    private BigDecimal pricePerHour;
    private BigDecimal startingSlotPrice;
    private BigDecimal maximumSlotPrice;
    private PitchType pitchType;
    private BoxType boxType;
    private TurfStatus status;
    private Double rating;
    private Integer totalReviews;
    private String sport;
    private List<String> sports;
    private List<String> amenities;
    private List<String> images;
}
