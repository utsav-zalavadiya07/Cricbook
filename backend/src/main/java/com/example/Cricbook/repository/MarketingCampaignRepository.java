package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MarketingCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, Long> {
    List<MarketingCampaign> findAllByOrderByCreatedAtDesc();
}
