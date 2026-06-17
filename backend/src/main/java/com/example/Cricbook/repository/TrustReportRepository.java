package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrustReport;
import com.example.Cricbook.enums.TrustReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrustReportRepository extends JpaRepository<TrustReport, Long> {
    List<TrustReport> findTop20ByReportedUserIdOrderByCreatedAtDesc(Long userId);
    List<TrustReport> findByStatusOrderByCreatedAtDesc(TrustReportStatus status);
    long countByReportedUserId(Long userId);
    long countByStatus(TrustReportStatus status);
}
