package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerStreak;
import com.example.Cricbook.enums.StreakType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerStreakRepository extends JpaRepository<PlayerStreak, Long> {
    Optional<PlayerStreak> findByUserIdAndStreakType(Long userId, StreakType streakType);
    List<PlayerStreak> findByUserIdOrderByStreakTypeAsc(Long userId);
    List<PlayerStreak> findTop10ByOrderByBestStreakDescUpdatedAtDesc();
}
