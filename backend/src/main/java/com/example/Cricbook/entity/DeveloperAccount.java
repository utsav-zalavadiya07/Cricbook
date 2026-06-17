package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "developer_accounts", indexes = {
        @Index(name = "idx_developer_status", columnList = "status"),
        @Index(name = "idx_developer_plan", columnList = "plan")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_developer_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String companyName;

    private String website;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String plan = "FREE";

    @Builder.Default
    private Boolean emailVerified = false;

    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(length = 1000)
    private String suspensionReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
