package com.example.Cricbook.repository;

import com.example.Cricbook.entity.NotificationAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationAnalyticsRepository extends JpaRepository<NotificationAnalytics, Long> {
    Optional<NotificationAnalytics> findByCampaignId(Long campaignId);
}
