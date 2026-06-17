package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrainingVideoAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingVideoAnalysisRepository extends JpaRepository<TrainingVideoAnalysis, Long> {
    List<TrainingVideoAnalysis> findAllByOrderByCreatedAtDesc();
    List<TrainingVideoAnalysis> findByPlayerEmailIgnoreCaseOrderByCreatedAtDesc(String playerEmail);
}
