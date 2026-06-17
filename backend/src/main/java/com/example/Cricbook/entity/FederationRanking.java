package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_rankings", indexes = {
        @Index(name = "idx_federation_ranking_body", columnList = "federationId"),
        @Index(name = "idx_federation_ranking_entity", columnList = "entityType,entityId"),
        @Index(name = "idx_federation_ranking_category", columnList = "rankingType,category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 40)
    private String rankingType;

    private String category;

    @Builder.Default
    private Integer points = 0;

    @Builder.Default
    private Integer rankPosition = 0;

    @Column(columnDefinition = "TEXT")
    private String calculationNotes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
