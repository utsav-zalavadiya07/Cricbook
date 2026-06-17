package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByDomain(String domain);

    boolean existsByDomainIgnoreCase(String domain);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByTenantCode(String tenantCode);

    List<Tenant> findAllByOrderByIdDesc();
}
