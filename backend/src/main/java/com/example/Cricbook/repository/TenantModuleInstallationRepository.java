package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantModuleInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantModuleInstallationRepository extends JpaRepository<TenantModuleInstallation, Long> {
    Optional<TenantModuleInstallation> findByTenantIdAndModuleId(Long tenantId, Long moduleId);

    List<TenantModuleInstallation> findByTenantIdOrderByInstalledAtDesc(Long tenantId);

    List<TenantModuleInstallation> findAllByOrderByInstalledAtDesc();
}
