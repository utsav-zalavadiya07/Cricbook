package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_profiles", indexes = {
        @Index(name = "idx_career_profile_status", columnList = "status"),
        @Index(name = "idx_career_profile_sport", columnList = "sport"),
        @Index(name = "idx_career_profile_city", columnList = "city"),
        @Index(name = "idx_career_profile_share_code", columnList = "shareCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false, unique = true)
    private String playerEmail;

    private String phone;

    @Column(nullable = false)
    private String sport;

    private String specialization;

    private String playerRole;

    private String city;

    private String ageGroup;

    private String profilePhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String statisticsSummary;

    @Column(columnDefinition = "TEXT")
    private String certificatesSummary;

    @Column(nullable = false)
    @Builder.Default
    private Integer profileCompleteness = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer rankingScore = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String rankingBand = "FOUNDATION";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String visibility = "PRIVATE";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false, unique = true)
    private String shareCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean scoutOptIn = false;

    @Column(nullable = false)
    @Builder.Default
    private Long profileViews = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long scoutViews = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long resumeDownloads = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
