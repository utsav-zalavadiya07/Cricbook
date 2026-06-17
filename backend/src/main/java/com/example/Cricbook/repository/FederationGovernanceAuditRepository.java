package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationGovernanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationGovernanceAuditRepository extends JpaRepository<FederationGovernanceAudit, Long> {
    List<FederationGovernanceAudit> findTop30ByOrderByCreatedAtDesc();
    List<FederationGovernanceAudit> findAllByOrderByCreatedAtDesc();
}
