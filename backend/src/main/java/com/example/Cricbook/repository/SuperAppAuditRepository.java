package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SuperAppAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuperAppAuditRepository extends JpaRepository<SuperAppAudit, Long> {
    List<SuperAppAudit> findAllByOrderByCreatedAtDesc();
}
