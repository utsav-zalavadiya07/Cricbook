package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeliveryMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryMetricRepository extends JpaRepository<DeliveryMetric, Long> {
    Optional<DeliveryMetric> findByNotificationId(Long notificationId);
}
