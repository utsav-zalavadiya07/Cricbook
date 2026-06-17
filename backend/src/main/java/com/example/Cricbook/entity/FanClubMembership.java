package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fan_club_memberships", indexes = {
        @Index(name = "idx_fan_club_membership_lookup", columnList = "fan_profile_id, fan_club_id, status"),
        @Index(name = "idx_fan_club_membership_club", columnList = "fan_club_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanClubMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fan_profile_id", nullable = false)
    private Long fanProfileId;

    @Column(name = "fan_club_id", nullable = false)
    private Long fanClubId;

    @Builder.Default
    @Column(nullable = false)
    private String role = "MEMBER";

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
