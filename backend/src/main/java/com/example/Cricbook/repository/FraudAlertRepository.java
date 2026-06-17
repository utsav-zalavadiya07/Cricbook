package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByUserId(Long userId);
    List<FraudAlert> findBySeverity(String severity);
}
