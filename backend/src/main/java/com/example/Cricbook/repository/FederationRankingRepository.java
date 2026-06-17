package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationRankingRepository extends JpaRepository<FederationRanking, Long> {
    List<FederationRanking> findAllByOrderByPointsDescCreatedAtDesc();
    List<FederationRanking> findByFederationIdOrderByPointsDescCreatedAtDesc(Long federationId);
}
