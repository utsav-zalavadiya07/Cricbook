package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "universal_wallet_ledgers", indexes = {
        @Index(name = "idx_universal_wallet_ledger_identity", columnList = "identity_id, created_at"),
        @Index(name = "idx_universal_wallet_ledger_wallet", columnList = "wallet_id, created_at"),
        @Index(name = "idx_universal_wallet_ledger_reference", columnList = "reference_type, reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversalWalletLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "identity_id", nullable = false)
    private Long identityId;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private String sourceModule;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    private String referenceType;

    private String referenceId;

    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
