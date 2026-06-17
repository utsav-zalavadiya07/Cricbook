package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAdminId(String adminId);
    List<AuditLog> findByAction(String action);
}
