package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantFeatureToggle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantFeatureToggleRepository extends JpaRepository<TenantFeatureToggle, Long> {
    Optional<TenantFeatureToggle> findByTenantIdAndFeatureKeyIgnoreCase(Long tenantId, String featureKey);

    List<TenantFeatureToggle> findByTenantIdOrderByFeatureKeyAsc(Long tenantId);

    List<TenantFeatureToggle> findAllByOrderByUpdatedAtDesc();
}
