package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanEconomyAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FanEconomyAuditRepository extends JpaRepository<FanEconomyAudit, Long> {
    List<FanEconomyAudit> findAllByOrderByCreatedAtDesc();
}
