package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sport_configuration_audits", indexes = {
        @Index(name = "idx_sport_config_audit_sport", columnList = "sport_id"),
        @Index(name = "idx_sport_config_audit_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SportConfigurationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_id")
    private Long sportId;

    @Column(name = "sport_code", length = 64)
    private String sportCode;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String actor;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
