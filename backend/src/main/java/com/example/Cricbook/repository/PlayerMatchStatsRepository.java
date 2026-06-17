package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerMatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerMatchStatsRepository extends JpaRepository<PlayerMatchStats, Long> {
    List<PlayerMatchStats> findByLiveScoreId(Long liveScoreId);
    List<PlayerMatchStats> findByUserId(Long userId);
    Optional<PlayerMatchStats> findByLiveScoreIdAndUserId(Long liveScoreId, Long userId);

    @Query("""
            SELECT stats FROM PlayerMatchStats stats
            JOIN FETCH stats.user
            JOIN FETCH stats.liveScore score
            WHERE stats.user.id = :userId
            ORDER BY COALESCE(score.updatedAt, stats.updatedAt, stats.createdAt) DESC
            """)
    List<PlayerMatchStats> findByUserIdWithScoreOrderByRecent(Long userId);

    @Query("""
            SELECT stats FROM PlayerMatchStats stats
            JOIN FETCH stats.user
            JOIN FETCH stats.liveScore score
            ORDER BY COALESCE(score.updatedAt, stats.updatedAt, stats.createdAt) DESC
            """)
    List<PlayerMatchStats> findAllWithUserAndScoreOrderByRecent();
}
