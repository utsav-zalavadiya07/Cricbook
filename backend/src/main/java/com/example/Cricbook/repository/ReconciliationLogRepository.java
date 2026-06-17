package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ReconciliationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationLogRepository extends JpaRepository<ReconciliationLog, Long> {
    List<ReconciliationLog> findTop100ByOrderByCreatedAtDesc();
}
