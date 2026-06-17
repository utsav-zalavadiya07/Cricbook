package com.example.Cricbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSlotRequest {
    @NotBlank
    private String startTime; // Format "HH:mm"
    @NotBlank
    private String endTime; // Format "HH:mm"
    @NotNull
    @Positive
    private Integer slotDuration; // In minutes
    @Positive
    private BigDecimal price;
    private String priceStrategy;
    private Boolean replaceExisting;
    private Boolean fullDay;
}
