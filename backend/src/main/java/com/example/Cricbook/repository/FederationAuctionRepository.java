package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationAuction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationAuctionRepository extends JpaRepository<FederationAuction, Long> {
    List<FederationAuction> findAllByOrderByCreatedAtDesc();
    List<FederationAuction> findByFederationIdOrderByCreatedAtDesc(Long federationId);
}
