package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "developer_api_keys", indexes = {
        @Index(name = "idx_dev_api_key_prefix", columnList = "key_prefix"),
        @Index(name = "idx_dev_api_key_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dev_api_key_hash", columnNames = "key_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private DeveloperApplication application;

    @Column(name = "key_prefix", nullable = false, length = 32)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 128)
    private String keyHash;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String environment = "SANDBOX";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime rotatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
