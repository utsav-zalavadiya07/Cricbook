package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TenantComplianceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantComplianceRequestRepository extends JpaRepository<TenantComplianceRequest, Long> {
    List<TenantComplianceRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<TenantComplianceRequest> findAllByOrderByCreatedAtDesc();
}
