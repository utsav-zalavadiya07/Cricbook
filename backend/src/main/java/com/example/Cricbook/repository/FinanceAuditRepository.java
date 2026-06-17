package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FinanceAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceAuditRepository extends JpaRepository<FinanceAudit, Long> {
    List<FinanceAudit> findTop100ByOrderByTimestampDesc();
}
