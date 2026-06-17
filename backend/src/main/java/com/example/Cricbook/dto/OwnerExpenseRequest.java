package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OwnerExpenseRequest {
    private String type;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String note;
}
