package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scout_profiles", indexes = {
        @Index(name = "idx_scout_status", columnList = "status"),
        @Index(name = "idx_scout_region", columnList = "region"),
        @Index(name = "idx_scout_sport", columnList = "sportSpecialization")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoutProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String scoutName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String organizationName;

    private String clubType;

    private String sportSpecialization;

    private String region;

    private String credentialsUrl;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_VERIFICATION";

    @Column(nullable = false)
    @Builder.Default
    private Boolean verifiedBadge = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
