package com.example.Cricbook.repository;

import com.example.Cricbook.entity.NotificationRetryQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRetryQueueRepository extends JpaRepository<NotificationRetryQueue, Long> {
    List<NotificationRetryQueue> findByStatusAndNextAttemptAtBefore(String status, LocalDateTime time);
    Optional<NotificationRetryQueue> findByNotificationId(Long notificationId);
}
