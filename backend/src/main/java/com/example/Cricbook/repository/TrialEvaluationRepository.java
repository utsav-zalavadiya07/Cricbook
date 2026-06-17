package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrialEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrialEvaluationRepository extends JpaRepository<TrialEvaluation, Long> {
    List<TrialEvaluation> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
    List<TrialEvaluation> findAllByOrderByCreatedAtDesc();
}
