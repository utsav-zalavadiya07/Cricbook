package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_player_registrations", indexes = {
        @Index(name = "idx_federation_player_sports_id", columnList = "sportsId"),
        @Index(name = "idx_federation_player_email", columnList = "email"),
        @Index(name = "idx_federation_player_status", columnList = "status"),
        @Index(name = "idx_federation_player_franchise", columnList = "currentFranchiseId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationPlayerRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long federationId;

    private Long careerProfileId;

    @Column(nullable = false, unique = true, length = 40)
    private String sportsId;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String sport;

    private String city;

    private String ageGroup;

    private LocalDate dateOfBirth;

    private String identityDocumentUrl;

    private Long currentFranchiseId;

    @Builder.Default
    private Integer eligibilityScore = 60;

    @Builder.Default
    private Integer rankingScore = 0;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String verificationStatus = "PENDING";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "REGISTERED";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
