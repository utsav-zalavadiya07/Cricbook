package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerChallenge;
import com.example.Cricbook.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerChallengeRepository extends JpaRepository<PlayerChallenge, Long> {
    Optional<PlayerChallenge> findByUserIdAndChallengeId(Long userId, Long challengeId);
    List<PlayerChallenge> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long countByStatus(ChallengeStatus status);
}
