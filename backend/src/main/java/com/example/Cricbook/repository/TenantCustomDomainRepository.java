package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantCustomDomain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantCustomDomainRepository extends JpaRepository<TenantCustomDomain, Long> {
    boolean existsByDomainIgnoreCase(String domain);

    Optional<TenantCustomDomain> findByDomainIgnoreCase(String domain);

    List<TenantCustomDomain> findAllByOrderByCreatedAtDesc();

    List<TenantCustomDomain> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
