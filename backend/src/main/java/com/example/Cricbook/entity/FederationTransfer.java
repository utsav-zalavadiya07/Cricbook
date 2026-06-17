package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_transfers", indexes = {
        @Index(name = "idx_federation_transfer_player", columnList = "playerRegistrationId"),
        @Index(name = "idx_federation_transfer_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private Long playerRegistrationId;

    private Long fromFranchiseId;

    @Column(nullable = false)
    private Long toFranchiseId;

    @Column(nullable = false, length = 40)
    private String transferType;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "REQUESTED";

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
