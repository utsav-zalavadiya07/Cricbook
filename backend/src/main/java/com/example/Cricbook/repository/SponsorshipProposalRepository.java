package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsorshipProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorshipProposalRepository extends JpaRepository<SponsorshipProposal, Long> {
    List<SponsorshipProposal> findAllByOrderByCreatedAtDesc();
    List<SponsorshipProposal> findByStatusOrderByCreatedAtDesc(String status);
}
