package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerAuditRepository extends JpaRepository<CareerAudit, Long> {
    List<CareerAudit> findTop100ByOrderByCreatedAtDesc();
}
