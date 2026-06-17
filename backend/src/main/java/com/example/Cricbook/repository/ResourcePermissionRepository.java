package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ResourcePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, Long> {
    List<ResourcePermission> findByUserId(Long userId);
}
