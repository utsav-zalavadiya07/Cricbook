package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ConfigurationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigurationAuditRepository extends JpaRepository<ConfigurationAudit, Long> {
    List<ConfigurationAudit> findTop100ByOrderByChangedAtDesc();
}
