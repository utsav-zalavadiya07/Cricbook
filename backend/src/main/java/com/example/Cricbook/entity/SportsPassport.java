package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_passports", indexes = {
        @Index(name = "idx_sports_passport_identity", columnList = "identity_id", unique = true),
        @Index(name = "idx_sports_passport_share", columnList = "share_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsPassport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_id", nullable = false, unique = true)
    private Long identityId;

    @Column(name = "share_code", nullable = false, unique = true, length = 64)
    private String shareCode;

    @Builder.Default
    @Column(nullable = false)
    private String visibility = "PUBLIC";

    @Column(columnDefinition = "TEXT")
    private String qrPayload;

    @Column(columnDefinition = "TEXT")
    private String statisticsSnapshot;

    @Column(columnDefinition = "TEXT")
    private String achievementsSnapshot;

    @Column(columnDefinition = "TEXT")
    private String membershipSnapshot;

    @Column(columnDefinition = "TEXT")
    private String reputationSnapshot;

    @Builder.Default
    @Column(nullable = false)
    private Integer viewCount = 0;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
