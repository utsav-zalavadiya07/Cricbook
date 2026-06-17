package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ResaleAnalyticsResponse {
    private Long activeListings;
    private Long soldListings;
    private Long expiredListings;
    private Long waitlistUsers;
    private BigDecimal marketplaceGmv;
    private BigDecimal recoveredRevenue;
    private Integer successRate;
    private List<ResaleListingResponse> recentListings;
}
