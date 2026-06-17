package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingHistoryRepository extends JpaRepository<PricingHistory, Long> {
    List<PricingHistory> findByTurfIdOrderByCreatedAtDesc(Long turfId);
}
