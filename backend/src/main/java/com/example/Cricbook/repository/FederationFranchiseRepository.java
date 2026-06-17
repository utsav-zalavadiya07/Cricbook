package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationFranchise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationFranchiseRepository extends JpaRepository<FederationFranchise, Long> {
    boolean existsByFederationIdAndNameIgnoreCase(Long federationId, String name);
    List<FederationFranchise> findByFederationIdOrderByCreatedAtDesc(Long federationId);
    List<FederationFranchise> findAllByOrderByCreatedAtDesc();
}
