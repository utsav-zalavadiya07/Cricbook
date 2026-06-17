package com.example.Cricbook.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSlotRequest {
    private String startTime;
    private String endTime;
    @Positive
    private BigDecimal price;
    private Boolean isActive;
}
