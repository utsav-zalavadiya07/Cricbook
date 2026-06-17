package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelAuditRepository extends JpaRepository<TravelAudit, Long> {
    List<TravelAudit> findTop25ByOrderByCreatedAtDesc();
}
