package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "enterprise_businesses", uniqueConstraints = {
        @UniqueConstraint(name = "unique_enterprise_business_name", columnNames = {"tenant_id", "name"})
}, indexes = {
        @Index(name = "idx_enterprise_business_owner", columnList = "owner_id"),
        @Index(name = "idx_enterprise_business_tenant", columnList = "tenant_id"),
        @Index(name = "idx_enterprise_business_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
