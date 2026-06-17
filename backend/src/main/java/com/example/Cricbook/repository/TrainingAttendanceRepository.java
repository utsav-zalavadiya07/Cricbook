package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrainingAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingAttendanceRepository extends JpaRepository<TrainingAttendance, Long> {
    List<TrainingAttendance> findAllByOrderByCreatedAtDesc();
    List<TrainingAttendance> findByEnrollmentIdOrderBySessionDateDesc(Long enrollmentId);
}
