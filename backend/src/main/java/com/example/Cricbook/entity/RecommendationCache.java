package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_caches", indexes = {
        @Index(name = "idx_rec_cache_user", columnList = "user_id, category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recommended_turf_ids", nullable = false)
    private String recommendedTurfIds; // Comma-separated turf IDs

    @Column(nullable = false)
    private String category; // FOR_YOU, NEARBY, WEEKEND

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
