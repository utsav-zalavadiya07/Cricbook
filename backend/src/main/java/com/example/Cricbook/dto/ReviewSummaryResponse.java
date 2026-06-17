package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {
    private Long turfId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingBreakdown;
    private Integer reputationScore;
    private Double ownerResponseRate;
    private Long flaggedReviews;
    private String reputationLabel;
}
