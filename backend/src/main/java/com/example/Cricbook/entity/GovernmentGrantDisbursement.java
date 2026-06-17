package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "government_grant_disbursements", indexes = {
        @Index(name = "idx_government_grant_disbursement_grant", columnList = "grant_application_id"),
        @Index(name = "idx_government_grant_disbursement_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernmentGrantDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long grantApplicationId;

    @Column(nullable = false)
    private String stageName;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false)
    private String status = "DISBURSED";

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime disbursedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
