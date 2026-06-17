package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommerceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommerceAuditRepository extends JpaRepository<CommerceAudit, Long> {
    List<CommerceAudit> findTop25ByOrderByCreatedAtDesc();
}
