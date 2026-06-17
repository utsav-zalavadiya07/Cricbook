package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "federation_officials", indexes = {
        @Index(name = "idx_federation_official_body", columnList = "federationId"),
        @Index(name = "idx_federation_official_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationOfficial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 40)
    private String officialType;

    private String certificationUrl;

    private LocalDate certificationValidUntil;

    @Builder.Default
    private Boolean available = true;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_VERIFICATION";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
