package com.example.Cricbook.repository;

import com.example.Cricbook.entity.StaffMaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffMaintenanceTicketRepository extends JpaRepository<StaffMaintenanceTicket, Long> {
    List<StaffMaintenanceTicket> findByAssignedStaffIdOrderByCreatedAtDesc(Long staffId);
    List<StaffMaintenanceTicket> findByCenterIdInOrderByCreatedAtDesc(List<Long> centerIds);
    List<StaffMaintenanceTicket> findByStatusOrderByCreatedAtDesc(String status);
}
