package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_logs", indexes = @Index(name = "idx_reconciliation_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 120)
    private String transactionId;

    @Column(name = "gateway_amount", nullable = false)
    private BigDecimal gatewayAmount;

    @Column(name = "system_amount", nullable = false)
    private BigDecimal systemAmount;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
