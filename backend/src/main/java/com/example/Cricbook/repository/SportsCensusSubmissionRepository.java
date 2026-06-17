package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsCensusSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsCensusSubmissionRepository extends JpaRepository<SportsCensusSubmission, Long> {
    List<SportsCensusSubmission> findAllByOrderByCreatedAtDesc();

    List<SportsCensusSubmission> findByCycleCodeIgnoreCaseOrderByCreatedAtDesc(String cycleCode);
}
