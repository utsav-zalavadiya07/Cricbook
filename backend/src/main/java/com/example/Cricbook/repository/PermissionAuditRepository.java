package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PermissionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionAuditRepository extends JpaRepository<PermissionAudit, Long> {
    List<PermissionAudit> findTop100ByOrderByTimestampDesc();
}
