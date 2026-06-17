package com.example.Cricbook.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long planId;
    private String planName;
    private String type;
    private BigDecimal price;
    private Integer durationDays;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean autoRenew;
    private Long turfId;
    private String turfName;
}
