package com.example.Cricbook.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SlotPriceOverrideRequest {
    @NotEmpty
    private List<Long> slotIds;
    @NotNull
    private LocalDate date;
    @NotNull
    @Positive
    private BigDecimal price;
    private String reason;
}
