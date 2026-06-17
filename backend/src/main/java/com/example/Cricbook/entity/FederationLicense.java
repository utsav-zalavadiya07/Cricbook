package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_licenses", indexes = {
        @Index(name = "idx_federation_license_holder", columnList = "holderType,holderId"),
        @Index(name = "idx_federation_license_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false, length = 40)
    private String holderType;

    @Column(nullable = false)
    private Long holderId;

    @Column(nullable = false, unique = true, length = 60)
    private String licenseNumber;

    @Column(nullable = false, length = 40)
    private String licenseType;

    private LocalDate validFrom;

    private LocalDate validUntil;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
