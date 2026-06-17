package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_identities", indexes = {
        @Index(name = "idx_sports_identity_sports_id", columnList = "sports_id", unique = true),
        @Index(name = "idx_sports_identity_email", columnList = "primary_email", unique = true),
        @Index(name = "idx_sports_identity_status", columnList = "status"),
        @Index(name = "idx_sports_identity_city_sport", columnList = "city, primary_sport")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "sports_id", nullable = false, unique = true, length = 40)
    private String sportsId;

    @Column(name = "primary_email", nullable = false, unique = true)
    private String primaryEmail;

    @Column(nullable = false)
    private String displayName;

    private String phone;

    private String city;

    @Column(nullable = false)
    private String primarySport;

    @Builder.Default
    private String ageGroup = "OPEN";

    private String guardianEmail;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(nullable = false)
    private Integer identityScore = 70;

    @Builder.Default
    @Column(nullable = false)
    private Integer reputationScore = 70;

    @Builder.Default
    @Column(nullable = false)
    private String profileVisibility = "PUBLIC";

    @Column(columnDefinition = "TEXT")
    private String linkedModules;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
