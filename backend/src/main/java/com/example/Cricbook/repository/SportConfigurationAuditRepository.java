package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportConfigurationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportConfigurationAuditRepository extends JpaRepository<SportConfigurationAudit, Long> {
    List<SportConfigurationAudit> findTop50ByOrderByCreatedAtDesc();
    List<SportConfigurationAudit> findBySportIdOrderByCreatedAtDesc(Long sportId);
}
