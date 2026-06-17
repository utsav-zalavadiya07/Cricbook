package com.example.Cricbook.service;

import com.example.Cricbook.entity.NotificationAnalytics;
import com.example.Cricbook.entity.DeliveryMetric;
import com.example.Cricbook.repository.NotificationAnalyticsRepository;
import com.example.Cricbook.repository.DeliveryMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAnalyticsService {

    private final NotificationAnalyticsRepository analyticsRepository;
    private final DeliveryMetricRepository metricRepository;

    @Transactional
    public void recordSend(Long campaignId) {
        if (campaignId == null) return;
        NotificationAnalytics na = getOrCreateAnalytics(campaignId);
        na.setSends(na.getSends() + 1);
        analyticsRepository.save(na);
    }

    @Transactional
    public void recordView(Long campaignId, Long notificationId) {
        // Record in delivery metric
        Optional<DeliveryMetric> dmOpt = metricRepository.findByNotificationId(notificationId);
        DeliveryMetric dm = dmOpt.orElse(DeliveryMetric.builder()
                .notificationId(notificationId)
                .build());
        
        if (!dm.isOpened()) {
            dm.setOpened(true);
            dm.setOpenedAt(LocalDateTime.now());
            metricRepository.save(dm);

            // Increment campaign views
            if (campaignId != null) {
                NotificationAnalytics na = getOrCreateAnalytics(campaignId);
                na.setViews(na.getViews() + 1);
                analyticsRepository.save(na);
            }
        }
    }

    @Transactional
    public void recordClick(Long campaignId, Long notificationId) {
        // Record in delivery metric
        Optional<DeliveryMetric> dmOpt = metricRepository.findByNotificationId(notificationId);
        DeliveryMetric dm = dmOpt.orElse(DeliveryMetric.builder()
                .notificationId(notificationId)
                .build());

        if (!dm.isClicked()) {
            dm.setClicked(true);
            dm.setClickedAt(LocalDateTime.now());
            metricRepository.save(dm);

            // Increment campaign clicks
            if (campaignId != null) {
                NotificationAnalytics na = getOrCreateAnalytics(campaignId);
                na.setClicks(na.getClicks() + 1);
                analyticsRepository.save(na);
            }
        }
    }

    @Transactional
    public void recordConversion(Long campaignId) {
        if (campaignId == null) return;
        NotificationAnalytics na = getOrCreateAnalytics(campaignId);
        na.setConversions(na.getConversions() + 1);
        analyticsRepository.save(na);
    }

    @Transactional(readOnly = true)
    public NotificationAnalytics getCampaignAnalytics(Long campaignId) {
        return getOrCreateAnalytics(campaignId);
    }

    private NotificationAnalytics getOrCreateAnalytics(Long campaignId) {
        return analyticsRepository.findByCampaignId(campaignId)
                .orElseGet(() -> analyticsRepository.save(NotificationAnalytics.builder()
                        .campaignId(campaignId)
                        .views(0L)
                        .clicks(0L)
                        .sends(0L)
                        .conversions(0L)
                        .build()));
    }
}
