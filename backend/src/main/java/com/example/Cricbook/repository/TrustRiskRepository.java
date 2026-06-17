package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrustRisk;
import com.example.Cricbook.enums.TrustRiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrustRiskRepository extends JpaRepository<TrustRisk, Long> {
    Optional<TrustRisk> findByUserId(Long userId);
    List<TrustRisk> findByRiskLevelOrderByUpdatedAtDesc(TrustRiskLevel riskLevel);
}
