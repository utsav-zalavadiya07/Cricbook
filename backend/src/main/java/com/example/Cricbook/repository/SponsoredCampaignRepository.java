package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsoredCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsoredCampaignRepository extends JpaRepository<SponsoredCampaign, Long> {
    List<SponsoredCampaign> findAllByOrderByCreatedAtDesc();
    List<SponsoredCampaign> findByStatusOrderByCreatedAtDesc(String status);
}
