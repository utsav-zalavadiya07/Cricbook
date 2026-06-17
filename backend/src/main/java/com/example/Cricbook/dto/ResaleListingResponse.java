package com.example.Cricbook.dto;

import com.example.Cricbook.enums.ResaleListingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ResaleListingResponse {
    private Long id;
    private Long bookingId;
    private Long sellerId;
    private String sellerName;
    private Long buyerId;
    private String buyerName;
    private Long turfId;
    private String turfName;
    private String city;
    private String location;
    private Long slotId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private BigDecimal originalAmount;
    private BigDecimal commissionAmount;
    private ResaleListingStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime soldAt;
    private String notes;
    private Integer sellerTrustScore;
    private Integer buyerTrustScore;
    private Integer recommendationScore;
    private String recommendationReason;
    private LocalDateTime createdAt;
}
