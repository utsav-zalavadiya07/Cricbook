package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessPolicyRepository extends JpaRepository<AccessPolicy, Long> {
    List<AccessPolicy> findByRoleId(Long roleId);
}
