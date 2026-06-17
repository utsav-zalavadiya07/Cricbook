package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrainingProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {
    List<TrainingProgram> findAllByOrderByCreatedAtDesc();
    List<TrainingProgram> findByStatusOrderByCreatedAtDesc(String status);
    List<TrainingProgram> findByAcademyIdOrderByCreatedAtDesc(Long academyId);
}
