package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByOrgId(Long orgId);
    List<Branch> findByManagerId(Long managerId);
}
