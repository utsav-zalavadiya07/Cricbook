package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateEngagementAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateEngagementAuditRepository extends JpaRepository<CorporateEngagementAudit, Long> {
    List<CorporateEngagementAudit> findAllByOrderByCreatedAtDesc();
    List<CorporateEngagementAudit> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
