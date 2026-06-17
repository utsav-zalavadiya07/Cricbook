package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "developer_webhook_endpoints", indexes = {
        @Index(name = "idx_dev_webhook_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperWebhookEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private DeveloperApplication application;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false, length = 128)
    private String secret;

    @Column(length = 1000)
    private String eventTypes;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @Builder.Default
    private Integer retryCount = 0;

    private LocalDateTime lastDeliveredAt;

    private LocalDateTime lastFailureAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
