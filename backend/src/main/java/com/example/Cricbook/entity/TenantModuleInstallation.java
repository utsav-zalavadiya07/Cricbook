package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_module_installations", indexes = {
        @Index(name = "idx_tenant_module_install_lookup", columnList = "tenant_id, module_id", unique = true),
        @Index(name = "idx_tenant_module_install_status", columnList = "tenant_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantModuleInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    @Builder.Default
    @Column(nullable = false)
    private String licenseType = "FREE";

    @Builder.Default
    @Column(nullable = false)
    private String status = "INSTALLED";

    @CreationTimestamp
    @Column(name = "installed_at", updatable = false)
    private LocalDateTime installedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
