package com.example.Cricbook.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long turfId;
    private String turfName;
    private Integer rating;
    private String title;
    private String comment;
    private String status;
    private String moderationReason;
    private Boolean verifiedBooking;
    private String ownerReply;
    private LocalDateTime ownerReplyAt;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
