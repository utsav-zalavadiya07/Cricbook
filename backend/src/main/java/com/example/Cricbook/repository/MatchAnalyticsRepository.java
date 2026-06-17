package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MatchAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchAnalyticsRepository extends JpaRepository<MatchAnalytics, Long> {
    List<MatchAnalytics> findByMatchId(Long matchId);
    Optional<MatchAnalytics> findByMatchIdAndMetric(Long matchId, String metric);
}
