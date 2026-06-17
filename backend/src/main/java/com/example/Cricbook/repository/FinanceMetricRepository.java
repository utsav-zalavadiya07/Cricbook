package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FinanceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinanceMetricRepository extends JpaRepository<FinanceMetric, Long> {

    Optional<FinanceMetric> findByMetric(String metric);
}
