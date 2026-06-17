package com.example.Cricbook.repository;

import com.example.Cricbook.entity.EnterpriseStaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnterpriseStaffAssignmentRepository extends JpaRepository<EnterpriseStaffAssignment, Long> {
    List<EnterpriseStaffAssignment> findByBusinessId(Long businessId);
    List<EnterpriseStaffAssignment> findByBusinessOwnerId(Long ownerId);
    List<EnterpriseStaffAssignment> findByStaffIdAndActiveTrue(Long staffId);
}
