package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperPlatformAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeveloperPlatformAuditRepository extends JpaRepository<DeveloperPlatformAudit, Long> {
    List<DeveloperPlatformAudit> findTop100ByOrderByCreatedAtDesc();
    List<DeveloperPlatformAudit> findByDeveloperIdOrderByCreatedAtDesc(Long developerId);
}
