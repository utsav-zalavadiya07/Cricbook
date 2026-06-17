package com.example.Cricbook.repository;

import com.example.Cricbook.entity.OwnerStaffRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OwnerStaffRoleRepository extends JpaRepository<OwnerStaffRole, Long> {
    List<OwnerStaffRole> findByOwnerId(Long ownerId);
    List<OwnerStaffRole> findByStaffIdAndActiveTrue(Long staffId);
}
