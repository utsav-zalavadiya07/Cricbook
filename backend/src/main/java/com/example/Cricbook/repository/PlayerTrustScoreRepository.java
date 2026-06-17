package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerTrustScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerTrustScoreRepository extends JpaRepository<PlayerTrustScore, Long> {
    Optional<PlayerTrustScore> findByUserId(Long userId);
}
