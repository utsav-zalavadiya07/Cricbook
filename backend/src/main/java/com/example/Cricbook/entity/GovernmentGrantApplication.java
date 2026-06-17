package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "government_grant_applications", indexes = {
        @Index(name = "idx_government_grant_code", columnList = "grant_code", unique = true),
        @Index(name = "idx_government_grant_status", columnList = "status"),
        @Index(name = "idx_government_grant_geo", columnList = "state_name, district_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentGrantApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grant_code", nullable = false, unique = true, length = 48)
    private String grantCode;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String applicantType;

    @Column(nullable = false)
    private String programType;

    @Column(nullable = false)
    private String stateName;

    @Column(nullable = false)
    private String districtName;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal requestedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 14, scale = 2)
    private BigDecimal disbursedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private String status = "SUBMITTED";

    @Column(columnDefinition = "TEXT")
    private String supportingDocuments;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
