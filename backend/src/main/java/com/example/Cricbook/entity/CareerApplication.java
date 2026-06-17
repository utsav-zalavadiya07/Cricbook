package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_applications", indexes = {
        @Index(name = "idx_career_app_status", columnList = "status"),
        @Index(name = "idx_career_app_opportunity", columnList = "opportunity_id"),
        @Index(name = "idx_career_app_profile", columnList = "profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private CareerOpportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private CareerProfile profile;

    @Column(nullable = false)
    private String applicationType;

    @Column(columnDefinition = "TEXT")
    private String statement;

    private String documentsUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "APPLIED";

    @Column(nullable = false)
    @Builder.Default
    private Integer evaluatorScore = 0;

    private String decisionNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
