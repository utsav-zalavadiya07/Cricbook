package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fan_profiles", indexes = {
        @Index(name = "idx_fan_profile_code", columnList = "fan_code", unique = true),
        @Index(name = "idx_fan_profile_email", columnList = "email", unique = true),
        @Index(name = "idx_fan_profile_status_rank", columnList = "status, rank_points")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fan_code", nullable = false, unique = true, length = 48)
    private String fanCode;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, unique = true)
    private String email;

    private String city;

    @Builder.Default
    @Column(nullable = false)
    private String favoriteSport = "CRICKET";

    private String favoriteTeam;

    private String favoritePlayer;

    private String avatarUrl;

    @Builder.Default
    @Column(nullable = false)
    private Integer fanScore = 0;

    @Builder.Default
    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints = 0;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
