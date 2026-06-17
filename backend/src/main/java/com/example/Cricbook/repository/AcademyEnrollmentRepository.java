package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AcademyEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademyEnrollmentRepository extends JpaRepository<AcademyEnrollment, Long> {
    List<AcademyEnrollment> findAllByOrderByCreatedAtDesc();
    List<AcademyEnrollment> findByStatusOrderByCreatedAtDesc(String status);
    List<AcademyEnrollment> findByBatchIdOrderByCreatedAtDesc(Long batchId);
    Optional<AcademyEnrollment> findByBatchIdAndStudentEmailIgnoreCase(Long batchId, String studentEmail);
}
