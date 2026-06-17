package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "universal_reward_ledgers", indexes = {
        @Index(name = "idx_universal_reward_ledger_identity", columnList = "identity_id, created_at"),
        @Index(name = "idx_universal_reward_ledger_account", columnList = "account_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversalRewardLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "identity_id", nullable = false)
    private Long identityId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String sourceModule;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Integer balanceAfter;

    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
