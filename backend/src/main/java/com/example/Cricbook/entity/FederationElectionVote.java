package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_election_votes", indexes = {
        @Index(name = "idx_federation_vote_election", columnList = "electionId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_federation_election_voter", columnNames = {"electionId", "voterReference"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationElectionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long electionId;

    @Column(nullable = false)
    private String voterReference;

    @Column(nullable = false)
    private String candidateName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
