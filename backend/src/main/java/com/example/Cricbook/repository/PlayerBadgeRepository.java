package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerBadgeRepository extends JpaRepository<PlayerBadge, Long> {
    Optional<PlayerBadge> findByUserIdAndBadge(Long userId, String badge);
    List<PlayerBadge> findByUserIdOrderByEarnedAtDesc(Long userId);
}
