package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantAppBuild;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantAppBuildRepository extends JpaRepository<TenantAppBuild, Long> {
    List<TenantAppBuild> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<TenantAppBuild> findAllByOrderByCreatedAtDesc();
}
