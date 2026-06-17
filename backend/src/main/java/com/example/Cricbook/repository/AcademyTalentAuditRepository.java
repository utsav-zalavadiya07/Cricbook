package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AcademyTalentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademyTalentAuditRepository extends JpaRepository<AcademyTalentAudit, Long> {
    List<AcademyTalentAudit> findTop100ByOrderByCreatedAtDesc();
}
