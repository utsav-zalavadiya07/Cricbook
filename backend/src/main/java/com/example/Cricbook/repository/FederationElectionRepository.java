package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationElection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationElectionRepository extends JpaRepository<FederationElection, Long> {
    List<FederationElection> findAllByOrderByCreatedAtDesc();
    List<FederationElection> findByFederationIdOrderByCreatedAtDesc(Long federationId);
}
