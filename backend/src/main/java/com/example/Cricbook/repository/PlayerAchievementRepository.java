package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {
    Optional<PlayerAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);
    List<PlayerAchievement> findByUserIdOrderByEarnedAtDesc(Long userId);
}
