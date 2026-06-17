package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "watch_parties", indexes = {
        @Index(name = "idx_watch_party_code", columnList = "party_code", unique = true),
        @Index(name = "idx_watch_party_status", columnList = "status, scheduled_at"),
        @Index(name = "idx_watch_party_host", columnList = "host_fan_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_code", nullable = false, unique = true, length = 48)
    private String partyCode;

    @Column(nullable = false)
    private String title;

    @Column(name = "host_fan_profile_id", nullable = false)
    private Long hostFanProfileId;

    @Builder.Default
    @Column(nullable = false)
    private String sport = "CRICKET";

    private Long matchId;

    private Long tournamentId;

    @Builder.Default
    @Column(nullable = false)
    private String platform = "IN_APP";

    @Builder.Default
    @Column(nullable = false)
    private Integer capacity = 50;

    @Builder.Default
    @Column(nullable = false)
    private Integer participantCount = 0;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Builder.Default
    @Column(nullable = false)
    private String accessMode = "PUBLIC";

    @Builder.Default
    @Column(nullable = false)
    private String status = "SCHEDULED";

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
