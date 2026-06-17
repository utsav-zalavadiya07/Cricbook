package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeveloperApiUsageRepository extends JpaRepository<DeveloperApiUsage, Long> {
    long countByApplication_Id(Long applicationId);
    long countByApplication_IdAndCreatedAtAfter(Long applicationId, LocalDateTime createdAt);
    List<DeveloperApiUsage> findTop100ByOrderByCreatedAtDesc();
    List<DeveloperApiUsage> findByApplication_IdOrderByCreatedAtDesc(Long applicationId);
}
