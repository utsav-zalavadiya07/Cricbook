package com.example.Cricbook.service;

import com.example.Cricbook.entity.NotificationQueue;
import com.example.Cricbook.entity.NotificationRetryQueue;
import com.example.Cricbook.repository.NotificationRepository;
import com.example.Cricbook.repository.NotificationRetryQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReliabilityEngine {

    private final NotificationRepository notificationRepository;
    private final NotificationRetryQueueRepository retryQueueRepository;
    private final EmailService emailService;

    // Rate Limiting and Deduplication Store
    private final Map<String, LocalDateTime> deduplicationCache = new HashMap<>();

    public boolean isDuplicate(Long userId, String message) {
        String key = userId + ":" + message.hashCode();
        LocalDateTime now = LocalDateTime.now();
        if (deduplicationCache.containsKey(key)) {
            LocalDateTime lastSent = deduplicationCache.get(key);
            if (now.isBefore(lastSent.plusMinutes(2))) { // 2 minutes window
                log.warn("Deduplicated identical notification for user {}: duplicate request rejected.", userId);
                return true;
            }
        }
        deduplicationCache.put(key, now);
        return false;
    }

    public void registerForRetry(Long notificationId) {
        Optional<NotificationRetryQueue> existing = retryQueueRepository.findByNotificationId(notificationId);
        if (existing.isPresent()) {
            NotificationRetryQueue r = existing.get();
            r.setAttempts(r.getAttempts() + 1);
            r.setNextAttemptAt(LocalDateTime.now().plusSeconds(30L * (1L << r.getAttempts()))); // Exponential backoff
            if (r.getAttempts() >= 3) {
                r.setStatus("DEAD");
                log.error("Notification {} reached maximum retries. Moved to Dead Letter Queue.", notificationId);
            }
            retryQueueRepository.save(r);
        } else {
            retryQueueRepository.save(NotificationRetryQueue.builder()
                    .notificationId(notificationId)
                    .attempts(1)
                    .nextAttemptAt(LocalDateTime.now().plusSeconds(30))
                    .status("PENDING")
                    .build());
            log.info("Registered notification {} for automated retry workflow", notificationId);
        }
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processRetries() {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationRetryQueue> retries = retryQueueRepository.findByStatusAndNextAttemptAtBefore("PENDING", now);

        if (retries.isEmpty()) return;
        log.info("Found {} failed notifications in retry queue. Retrying...", retries.size());

        for (NotificationRetryQueue r : retries) {
            Optional<NotificationQueue> nqOpt = notificationRepository.findById(r.getNotificationId());
            if (nqOpt.isPresent()) {
                NotificationQueue n = nqOpt.get();
                try {
                    // Re-execute sending
                    if (n.getChannel() == com.example.Cricbook.enums.NotificationChannel.EMAIL) {
                        emailService.send(n.getEmail(), n.getTitle(), n.getMessage());
                    } else {
                        log.info("Simulated channel {} retry for notification ID {}", n.getChannel(), n.getId());
                    }
                    n.setStatus("SENT");
                    notificationRepository.save(n);
                    
                    // Mark retry as processed
                    retryQueueRepository.delete(r);
                    log.info("Successfully retried and sent notification ID: {}", n.getId());
                } catch (Exception e) {
                    log.error("Retry attempt #{} failed for notification ID: {}", r.getAttempts() + 1, n.getId(), e);
                    r.setAttempts(r.getAttempts() + 1);
                    r.setNextAttemptAt(LocalDateTime.now().plusSeconds(30L * (1L << r.getAttempts())));
                    if (r.getAttempts() >= 3) {
                        r.setStatus("DEAD");
                        n.setStatus("FAILED");
                        notificationRepository.save(n);
                        log.error("Notification ID {} permanently failed. Sent to DLQ.", n.getId());
                    }
                    retryQueueRepository.save(r);
                }
            } else {
                retryQueueRepository.delete(r);
            }
        }
    }
}
