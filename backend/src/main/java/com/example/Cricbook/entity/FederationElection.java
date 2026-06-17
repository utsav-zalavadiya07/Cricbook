package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_elections", indexes = {
        @Index(name = "idx_federation_election_body", columnList = "federationId"),
        @Index(name = "idx_federation_election_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationElection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String positionName;

    @Column(length = 2000)
    private String candidateNames;

    private LocalDateTime votingOpensAt;

    private LocalDateTime votingClosesAt;

    private String winnerName;

    @Builder.Default
    private Integer voteCount = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
