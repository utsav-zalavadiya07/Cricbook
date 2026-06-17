package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "banned_entities", indexes = @Index(name = "idx_banned_entity_type_value", columnList = "type,entity_value"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(name = "entity_value", nullable = false, length = 180)
    private String value;

    @Column(nullable = false, length = 800)
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
