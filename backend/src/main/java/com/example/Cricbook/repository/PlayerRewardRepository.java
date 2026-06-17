package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerReward;
import com.example.Cricbook.enums.GamificationRewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRewardRepository extends JpaRepository<PlayerReward, Long> {
    List<PlayerReward> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long countByStatus(GamificationRewardStatus status);
}
