package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TemporaryPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TemporaryPermissionRepository extends JpaRepository<TemporaryPermission, Long> {
    List<TemporaryPermission> findByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);
    long deleteByExpiresAtBefore(LocalDateTime now);
}
