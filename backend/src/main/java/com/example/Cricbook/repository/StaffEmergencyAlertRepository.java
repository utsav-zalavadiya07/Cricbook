package com.example.Cricbook.repository;

import com.example.Cricbook.entity.StaffEmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffEmergencyAlertRepository extends JpaRepository<StaffEmergencyAlert, Long> {
    List<StaffEmergencyAlert> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<StaffEmergencyAlert> findByCenterIdInOrderByCreatedAtDesc(List<Long> centerIds);
    List<StaffEmergencyAlert> findByStatusOrderByCreatedAtDesc(String status);
}
