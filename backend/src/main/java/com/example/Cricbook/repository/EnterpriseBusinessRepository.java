package com.example.Cricbook.repository;

import com.example.Cricbook.entity.EnterpriseBusiness;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnterpriseBusinessRepository extends JpaRepository<EnterpriseBusiness, Long> {
    List<EnterpriseBusiness> findByOwnerId(Long ownerId);
    List<EnterpriseBusiness> findByTenantId(Long tenantId);
    Optional<EnterpriseBusiness> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);
}
