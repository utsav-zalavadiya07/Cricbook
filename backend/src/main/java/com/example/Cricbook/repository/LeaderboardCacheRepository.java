package com.example.Cricbook.repository;

import com.example.Cricbook.entity.LeaderboardCache;
import com.example.Cricbook.enums.LeaderboardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardCacheRepository extends JpaRepository<LeaderboardCache, Long> {
    List<LeaderboardCache> findTop10ByLeaderboardTypeAndCityIgnoreCaseOrderByRankAsc(LeaderboardType leaderboardType, String city);
}
