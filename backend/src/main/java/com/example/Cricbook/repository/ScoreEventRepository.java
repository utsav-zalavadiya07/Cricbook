package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreEventRepository extends JpaRepository<ScoreEvent, Long> {
    List<ScoreEvent> findByLiveScoreIdOrderByEventSequenceAsc(Long liveScoreId);
    long countByLiveScoreId(Long liveScoreId);
}
