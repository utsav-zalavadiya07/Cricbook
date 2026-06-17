package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fan_collectible_ownerships", indexes = {
        @Index(name = "idx_fan_collectible_owner", columnList = "fan_profile_id, collectible_id, ownership_status"),
        @Index(name = "idx_fan_collectible_ref", columnList = "ownership_reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanCollectibleOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collectible_id", nullable = false)
    private Long collectibleId;

    @Column(name = "fan_profile_id", nullable = false)
    private Long fanProfileId;

    @Column(name = "ownership_reference", nullable = false, unique = true, length = 96)
    private String ownershipReference;

    @Column(nullable = false)
    private Integer serialNumber;

    @Builder.Default
    @Column(name = "ownership_status", nullable = false)
    private String ownershipStatus = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String transferHistory;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "acquired_at", updatable = false)
    private LocalDateTime acquiredAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
