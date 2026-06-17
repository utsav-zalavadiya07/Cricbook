package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantMarketplaceModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantMarketplaceModuleRepository extends JpaRepository<TenantMarketplaceModule, Long> {
    boolean existsByModuleCodeIgnoreCase(String moduleCode);

    Optional<TenantMarketplaceModule> findByModuleCodeIgnoreCase(String moduleCode);

    List<TenantMarketplaceModule> findByStatusOrderByCreatedAtDesc(String status);

    List<TenantMarketplaceModule> findAllByOrderByCreatedAtDesc();
}
