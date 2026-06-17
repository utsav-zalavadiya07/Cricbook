package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayabilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PlayabilityScoreRepository extends JpaRepository<PlayabilityScore, Long> {
    Optional<PlayabilityScore> findFirstBySlotIdAndPlayDateOrderByUpdatedAtDesc(Long slotId, LocalDate playDate);
}
