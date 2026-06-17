package com.example.Cricbook.repository;

import com.example.Cricbook.entity.StaffTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffTaskRepository extends JpaRepository<StaffTask, Long> {
    List<StaffTask> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<StaffTask> findByCenterIdInOrderByCreatedAtDesc(List<Long> centerIds);
    List<StaffTask> findByStatusOrderByCreatedAtDesc(String status);
}
