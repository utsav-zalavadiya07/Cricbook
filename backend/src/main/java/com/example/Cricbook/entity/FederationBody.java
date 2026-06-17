package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_bodies", indexes = {
        @Index(name = "idx_federation_body_type", columnList = "bodyType"),
        @Index(name = "idx_federation_body_status", columnList = "status"),
        @Index(name = "idx_federation_body_parent", columnList = "parentBodyId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationBody {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentBodyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 40)
    private String bodyType;

    @Column(nullable = false)
    private String sport;

    private String region;

    private String registrationNumber;

    private String website;

    private String contactEmail;

    @Column(columnDefinition = "TEXT")
    private String governanceNotes;

    @Builder.Default
    private Integer affiliatedMembers = 0;

    @Builder.Default
    private Integer complianceScore = 75;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
