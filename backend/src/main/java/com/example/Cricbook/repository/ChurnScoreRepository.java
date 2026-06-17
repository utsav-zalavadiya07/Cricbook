package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ChurnScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChurnScoreRepository extends JpaRepository<ChurnScore, Long> {
    Optional<ChurnScore> findByUserId(Long userId);
}
