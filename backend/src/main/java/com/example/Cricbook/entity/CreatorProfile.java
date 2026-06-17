package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "creator_profiles", indexes = {
        @Index(name = "idx_creator_profile_email", columnList = "email", unique = true),
        @Index(name = "idx_creator_profile_category", columnList = "creator_category, verification_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    private String city;

    @Builder.Default
    @Column(name = "creator_category", nullable = false)
    private String creatorCategory = "FAN_CREATOR";

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profileUrl;

    @Builder.Default
    @Column(name = "verification_status", nullable = false)
    private String verificationStatus = "PENDING";

    @Builder.Default
    @Column(nullable = false)
    private Boolean monetizationEnabled = false;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private BigDecimal revenueBalance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
