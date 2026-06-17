package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "career_achievements", indexes = {
        @Index(name = "idx_career_achievement_profile", columnList = "profile_id"),
        @Index(name = "idx_career_achievement_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CareerProfile profile;

    @Column(nullable = false)
    private String title;

    private String achievementType;

    private String issuer;

    private LocalDate eventDate;

    private String evidenceUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_VERIFICATION";

    private String verifiedBy;

    private LocalDateTime verifiedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
