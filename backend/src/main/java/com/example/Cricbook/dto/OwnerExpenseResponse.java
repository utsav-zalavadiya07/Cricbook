package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class OwnerExpenseResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String note;
}
