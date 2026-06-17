package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, Long> {
    List<RecommendationCache> findByUserId(Long userId);
    Optional<RecommendationCache> findByUserIdAndCategory(Long userId, String category);
    void deleteByUserId(Long userId);
}
