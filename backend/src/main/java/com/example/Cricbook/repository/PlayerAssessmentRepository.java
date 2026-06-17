package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PlayerAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerAssessmentRepository extends JpaRepository<PlayerAssessment, Long> {
    List<PlayerAssessment> findAllByOrderByCreatedAtDesc();
    List<PlayerAssessment> findByPlayerEmailIgnoreCaseOrderByCreatedAtDesc(String playerEmail);
}
