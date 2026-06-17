package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateWellnessActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateWellnessActivityRepository extends JpaRepository<CorporateWellnessActivity, Long> {
    List<CorporateWellnessActivity> findAllByOrderByCreatedAtDesc();
    List<CorporateWellnessActivity> findByProgramIdOrderByCreatedAtDesc(Long programId);
    List<CorporateWellnessActivity> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
