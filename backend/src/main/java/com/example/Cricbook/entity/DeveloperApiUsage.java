package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "developer_api_usage", indexes = {
        @Index(name = "idx_dev_usage_application", columnList = "application_id"),
        @Index(name = "idx_dev_usage_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperApiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private DeveloperApplication application;

    @Column(length = 32)
    private String keyPrefix;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false, length = 20)
    private String method;

    private Integer statusCode;

    private Long latencyMs;

    private String errorCategory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
