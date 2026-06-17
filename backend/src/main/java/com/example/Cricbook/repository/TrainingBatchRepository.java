package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrainingBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingBatchRepository extends JpaRepository<TrainingBatch, Long> {
    List<TrainingBatch> findAllByOrderByCreatedAtDesc();
    List<TrainingBatch> findByProgramIdOrderByCreatedAtDesc(Long programId);
}
