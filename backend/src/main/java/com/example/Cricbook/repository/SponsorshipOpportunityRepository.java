package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsorshipOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorshipOpportunityRepository extends JpaRepository<SponsorshipOpportunity, Long> {
    List<SponsorshipOpportunity> findAllByOrderByCreatedAtDesc();
    List<SponsorshipOpportunity> findByStatusOrderByCreatedAtDesc(String status);
}
