package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewResponse {
    private Long id;
    private Long userId;
    private Long turfId;
    private String turfName;
    private Integer rating;
    private String title;
    private String comment;
    private String status;
    private String moderationReason;
    private String ownerReply;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private LocalDateTime createdAt;
}
