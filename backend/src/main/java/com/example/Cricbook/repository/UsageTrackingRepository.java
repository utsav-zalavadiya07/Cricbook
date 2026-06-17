package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, Long> {

    Optional<UsageTracking> findByUserIdAndFeatureAndPeriodMonth(Long userId, String feature, String periodMonth);
}
