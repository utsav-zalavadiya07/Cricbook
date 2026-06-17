package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "review_reports",
        indexes = @Index(name = "idx_review_report_review_reporter", columnList = "review_id,reporter_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_review_report_once_per_user", columnNames = {"review_id", "reporter_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status; // PENDING, RESOLVED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
