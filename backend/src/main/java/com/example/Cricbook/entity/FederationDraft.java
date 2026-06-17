package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_drafts", indexes = {
        @Index(name = "idx_federation_draft_body", columnList = "federationId"),
        @Index(name = "idx_federation_draft_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    private String season;

    @Column(length = 2000)
    private String draftOrderFranchiseIds;

    @Builder.Default
    private Integer poolSize = 0;

    @Builder.Default
    private Integer completedPicks = 0;

    private LocalDateTime startsAt;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
