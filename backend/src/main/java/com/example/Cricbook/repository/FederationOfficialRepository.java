package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationOfficial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationOfficialRepository extends JpaRepository<FederationOfficial, Long> {
    List<FederationOfficial> findAllByOrderByCreatedAtDesc();
    List<FederationOfficial> findByFederationIdOrderByCreatedAtDesc(Long federationId);
}
