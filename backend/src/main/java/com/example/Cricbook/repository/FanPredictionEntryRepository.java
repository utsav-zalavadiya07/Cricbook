package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanPredictionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FanPredictionEntryRepository extends JpaRepository<FanPredictionEntry, Long> {
    Optional<FanPredictionEntry> findByGameIdAndFanProfileId(Long gameId, Long fanProfileId);

    List<FanPredictionEntry> findByGameIdOrderByCreatedAtDesc(Long gameId);

    List<FanPredictionEntry> findAllByOrderByCreatedAtDesc();
}
