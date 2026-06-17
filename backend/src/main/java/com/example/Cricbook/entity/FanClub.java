package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fan_clubs", indexes = {
        @Index(name = "idx_fan_club_code", columnList = "club_code", unique = true),
        @Index(name = "idx_fan_club_sport_status", columnList = "sport, status"),
        @Index(name = "idx_fan_club_entity", columnList = "linked_entity_type, linked_entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_code", nullable = false, unique = true, length = 48)
    private String clubCode;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private String sport = "CRICKET";

    @Builder.Default
    @Column(nullable = false)
    private String clubType = "TEAM";

    @Column(name = "linked_entity_type")
    private String linkedEntityType;

    @Column(name = "linked_entity_id")
    private Long linkedEntityId;

    private String city;

    @Builder.Default
    @Column(nullable = false)
    private String visibility = "PUBLIC";

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(nullable = false)
    private Integer memberCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
