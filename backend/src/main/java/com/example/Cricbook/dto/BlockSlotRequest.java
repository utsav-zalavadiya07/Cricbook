package com.example.Cricbook.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BlockSlotRequest {
    @NotNull
    private LocalDate date;
    @NotNull
    @NotEmpty
    private List<Long> slotIds;
    private Boolean blocked;
}
