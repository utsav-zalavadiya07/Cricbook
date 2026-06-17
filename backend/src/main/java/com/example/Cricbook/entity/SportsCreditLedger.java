package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sports_credit_ledgers", indexes = {
        @Index(name = "idx_sports_credit_identity", columnList = "identity_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportsCreditLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_id", nullable = false)
    private Long identityId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String sourceModule;

    @Column(nullable = false)
    private Integer credits;

    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
