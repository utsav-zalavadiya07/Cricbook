package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "turf_id"}), indexes = {
        @Index(name = "idx_review_turf_status", columnList = "turf_id,status"),
        @Index(name = "idx_review_user_status", columnList = "user_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "turf_id", nullable = false)
    private Long turfId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 140)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "moderation_reason", length = 500)
    private String moderationReason;

    @Column(name = "owner_reply", columnDefinition = "TEXT")
    private String ownerReply;

    @Column(name = "owner_reply_at")
    private LocalDateTime ownerReplyAt;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "not_helpful_count", nullable = false)
    @Builder.Default
    private Integer notHelpfulCount = 0;

    @Column(name = "verified_booking", nullable = false)
    @Builder.Default
    private Boolean verifiedBooking = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
