package com.example.Cricbook.repository;

import com.example.Cricbook.entity.EnterpriseFranchise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnterpriseFranchiseRepository extends JpaRepository<EnterpriseFranchise, Long> {
    List<EnterpriseFranchise> findByBusinessId(Long businessId);
    List<EnterpriseFranchise> findByBusinessOwnerId(Long ownerId);
}
