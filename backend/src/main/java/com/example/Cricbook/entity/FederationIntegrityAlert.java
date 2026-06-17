package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "federation_integrity_alerts", indexes = {
        @Index(name = "idx_federation_integrity_body", columnList = "federationId"),
        @Index(name = "idx_federation_integrity_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FederationIntegrityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long federationId;

    @Column(nullable = false)
    private String alertType;

    @Column(nullable = false, length = 40)
    private String severity;

    @Column(nullable = false)
    private String subjectReference;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Builder.Default
    private Integer riskScore = 50;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "OPEN";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
