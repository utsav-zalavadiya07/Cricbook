package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "developer_applications", indexes = {
        @Index(name = "idx_dev_app_environment", columnList = "environment"),
        @Index(name = "idx_dev_app_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dev_app_client_id", columnNames = "client_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private DeveloperAccount developer;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false, length = 96)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 128)
    private String clientSecretHash;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String environment = "SANDBOX";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    private String callbackUrl;

    @Column(length = 1000)
    private String scopes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
