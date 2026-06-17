package com.example.Cricbook.dto;

import com.example.Cricbook.enums.BoxType;
import com.example.Cricbook.enums.PitchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateTurfRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 2000)
    private String description;
    @NotBlank
    @Size(max = 255)
    private String location;
    @NotBlank
    @Size(max = 100)
    private String city;
    private String country;
    private String state;
    private String timezone;
    private String holidaySource;
    @NotNull
    @Min(1)
    @Max(200)
    private Integer capacity;
    private Double latitude;
    private Double longitude;
    @NotNull
    @Positive
    private BigDecimal pricePerHour;
    @NotNull
    private PitchType pitchType;
    @NotNull
    private BoxType boxType;
    private String sport;
    private String sportCode;
    private List<String> sports;
    private List<String> amenities;
    private List<String> images;
}
