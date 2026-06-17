package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByScoreDesc(Long userId);
    void deleteByUserId(Long userId);
}
