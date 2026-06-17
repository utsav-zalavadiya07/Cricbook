package com.example.Cricbook.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateResaleListingRequest {
    private Long bookingId;
    private BigDecimal price;
    private LocalDateTime expiresAt;
    private String notes;
}
