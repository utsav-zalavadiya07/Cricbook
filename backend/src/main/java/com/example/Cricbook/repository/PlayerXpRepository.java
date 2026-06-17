package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerXp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerXpRepository extends JpaRepository<PlayerXp, Long> {
    Optional<PlayerXp> findByUserId(Long userId);
    List<PlayerXp> findTop10ByOrderByXpDescUpdatedAtDesc();
}
