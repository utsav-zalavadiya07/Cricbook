package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsoredAdEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsoredAdEventRepository extends JpaRepository<SponsoredAdEvent, Long> {
    long countByEventType(String eventType);
    long countByCampaignIdAndEventType(Long campaignId, String eventType);
    List<SponsoredAdEvent> findAllByOrderByCreatedAtDesc();
}
