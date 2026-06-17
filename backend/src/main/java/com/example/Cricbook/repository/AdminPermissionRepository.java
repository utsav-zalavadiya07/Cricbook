package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {
    Optional<AdminPermission> findByCode(String code);
    List<AdminPermission> findByCodeIn(Collection<String> codes);
    boolean existsByCode(String code);
}
