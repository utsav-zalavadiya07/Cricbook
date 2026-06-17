package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_audit", indexes = @Index(name = "idx_config_audit_key", columnList = "config_key"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "old_value", length = 2000)
    private String oldValue;

    @Column(name = "new_value", nullable = false, length = 2000)
    private String newValue;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
