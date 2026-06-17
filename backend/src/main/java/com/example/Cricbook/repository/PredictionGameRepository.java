package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PredictionGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PredictionGameRepository extends JpaRepository<PredictionGame, Long> {
    boolean existsByGameCode(String gameCode);

    List<PredictionGame> findAllByOrderByCreatedAtDesc();

    List<PredictionGame> findByStatusOrderByCreatedAtDesc(String status);
}
