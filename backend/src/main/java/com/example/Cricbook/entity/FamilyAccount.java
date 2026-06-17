package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "family_accounts", indexes = {
        @Index(name = "idx_family_parent", columnList = "parent_identity_id"),
        @Index(name = "idx_family_child", columnList = "child_identity_id"),
        @Index(name = "idx_family_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_identity_id", nullable = false)
    private Long parentIdentityId;

    @Column(name = "child_identity_id", nullable = false)
    private Long childIdentityId;

    @Column(nullable = false)
    private String guardianEmail;

    @Builder.Default
    @Column(nullable = false)
    private String relationshipType = "GUARDIAN";

    @Builder.Default
    @Column(nullable = false)
    private Boolean approvalRequired = true;

    @Builder.Default
    @Column(nullable = false)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
