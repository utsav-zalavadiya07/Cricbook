package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsOsAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsOsAuditRepository extends JpaRepository<SportsOsAudit, Long> {
    List<SportsOsAudit> findAllByOrderByCreatedAtDesc();
}
