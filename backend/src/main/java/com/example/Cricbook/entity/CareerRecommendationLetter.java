package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_recommendation_letters", indexes = {
        @Index(name = "idx_career_rec_profile", columnList = "profile_id"),
        @Index(name = "idx_career_rec_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerRecommendationLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CareerProfile profile;

    @Column(nullable = false)
    private String recommenderName;

    private String organization;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
