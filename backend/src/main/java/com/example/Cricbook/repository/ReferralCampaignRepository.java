package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ReferralCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralCampaignRepository extends JpaRepository<ReferralCampaign, Long> {
    List<ReferralCampaign> findAllByOrderByCreatedAtDesc();
}
