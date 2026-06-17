package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantSaasAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantSaasAuditRepository extends JpaRepository<TenantSaasAudit, Long> {
    List<TenantSaasAudit> findAllByOrderByCreatedAtDesc();

    List<TenantSaasAudit> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
