package com.example.Cricbook.service;

import com.example.Cricbook.dto.NotificationDtos;
import com.example.Cricbook.entity.NotificationQueue;
import com.example.Cricbook.entity.NotificationPreference;
import com.example.Cricbook.entity.DeliveryLog;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.repository.NotificationRepository;
import com.example.Cricbook.repository.NotificationPreferenceRepository;
import com.example.Cricbook.repository.DeliveryLogRepository;
import com.example.Cricbook.repository.NotificationAnalyticsRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;
    private final EmailService emailService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final NotificationAnalyticsService analyticsService;
    private final NotificationAnalyticsRepository notificationAnalyticsRepository;
    private final UserRepository userRepository;

    @Autowired
    @Lazy
    private ReliabilityEngine reliabilityEngine;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    public void pushNotification(Long userId, String email, String title, String message) {
        pushNotification(userId, email, title, message, NotificationChannel.EMAIL, null, null);
    }

    public void pushNotification(Long userId, String email, String title, String message, NotificationChannel channel, Long campaignId, String deepLink) {
        pushNotification(userId, email, title, message, channel, campaignId, deepLink, "GENERAL", "NORMAL");
    }

    @Transactional
    public void pushNotification(Long userId, String email, String title, String message, NotificationChannel channel, Long campaignId, String deepLink, String category, String priority) {
        if (userId == null) {
            throw new IllegalArgumentException("Notification recipient is required.");
        }
        String resolvedTitle = requireText(title, "Notification title is required.");
        String resolvedMessage = requireText(message, "Notification message is required.");

        // 1. Rate Limit & Deduplication Check
        if (reliabilityEngine != null && reliabilityEngine.isDuplicate(userId, resolvedMessage)) {
            log.info("Discarded duplicate notification for user ID: {}", userId);
            return;
        }

        // 2. Preferences Check
        NotificationChannel resolvedChannel = channel == null ? NotificationChannel.EMAIL : channel;
        String resolvedCategory = normalize(category, "GENERAL");
        String resolvedPriority = normalize(priority, "NORMAL");
        Optional<NotificationPreference> pref = preferenceRepository.findByUserIdAndChannel(userId, resolvedChannel);
        if (pref.isPresent() && !pref.get().isEnabled()) {
            log.info("Skipped pushing notification: channel {} is disabled by user preferences", resolvedChannel);
            return;
        }
        if (pref.isPresent() && isCategoryDisabled(pref.get(), resolvedCategory)) {
            log.info("Skipped pushing notification: category {} is disabled for channel {}", resolvedCategory, resolvedChannel);
            return;
        }

        NotificationQueue nq = NotificationQueue.builder()
                .userId(userId)
                .email(email)
                .title(resolvedTitle)
                .message(resolvedMessage)
                .channel(resolvedChannel)
                .campaignId(campaignId)
                .deepLink(deepLink)
                .category(resolvedCategory)
                .priority(resolvedPriority)
                .status("PENDING")
                .retryCount(0)
                .build();

        repository.save(nq);
        log.info("Pushed notification to queue for user: {}", email);
    }

    @Transactional
    public int pushToChannels(Long userId, String email, String title, String message, List<NotificationChannel> channels, Long campaignId, String deepLink, String category, String priority) {
        List<NotificationChannel> resolvedChannels = channels == null || channels.isEmpty()
                ? List.of(NotificationChannel.PUSH)
                : channels.stream().filter(Objects::nonNull).distinct().toList();
        int before = resolvedChannels.size();
        for (NotificationChannel channel : resolvedChannels) {
            pushNotification(userId, email, title, message, channel, campaignId, deepLink, category, priority);
        }
        return before;
    }

    @Transactional
    public NotificationDtos.BroadcastResponse broadcast(NotificationDtos.BroadcastRequest request) {
        String title = requireText(request == null ? null : request.getTitle(), "Broadcast title is required.");
        String message = requireText(request == null ? null : request.getMessage(), "Broadcast message is required.");
        String category = normalize(request == null ? null : request.getCategory(), "GENERAL");
        String priority = normalize(request == null ? null : request.getPriority(), "NORMAL");
        String targetRole = normalize(request == null ? null : request.getTargetRole(), "");
        Set<Long> targetUserIds = request == null || request.getTargetUserIds() == null
                ? Set.of()
                : request.getTargetUserIds().stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        List<NotificationChannel> channels = parseChannels(request == null ? null : request.getChannels());
        List<User> recipients = userRepository.findAll().stream()
                .filter(user -> targetUserIds.isEmpty() || targetUserIds.contains(user.getId()))
                .filter(user -> targetRole.isBlank() || userHasRole(user, targetRole))
                .limit(500)
                .toList();

        int queuedCount = 0;
        for (User user : recipients) {
            queuedCount += pushToChannels(
                    user.getId(),
                    user.getEmail(),
                    title,
                    message,
                    channels,
                    request == null ? null : request.getCampaignId(),
                    request == null ? null : request.getDeepLink(),
                    category,
                    priority
            );
        }
        return NotificationDtos.BroadcastResponse.builder()
                .recipientCount(recipients.size())
                .queuedCount(queuedCount)
                .channels(channels.stream().map(NotificationChannel::name).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationDtos.NotificationAnalyticsResponse platformAnalytics() {
        List<NotificationQueue> notifications = repository.findAll();
        long total = notifications.size();
        long sent = notifications.stream().filter(notification -> "SENT".equalsIgnoreCase(status(notification))).count();
        long pending = notifications.stream().filter(notification -> "PENDING".equalsIgnoreCase(status(notification))).count();
        long failed = notifications.stream().filter(notification -> "FAILED".equalsIgnoreCase(status(notification))).count();
        long read = notifications.stream().filter(this::isRead).count();
        long archived = notifications.stream().filter(this::isArchived).count();
        long views = notificationAnalyticsRepository.findAll().stream()
                .map(com.example.Cricbook.entity.NotificationAnalytics::getViews)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        long clicks = notificationAnalyticsRepository.findAll().stream()
                .map(com.example.Cricbook.entity.NotificationAnalytics::getClicks)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        long conversions = notificationAnalyticsRepository.findAll().stream()
                .map(com.example.Cricbook.entity.NotificationAnalytics::getConversions)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();

        Map<String, Long> byChannel = notifications.stream()
                .collect(Collectors.groupingBy(notification -> notification.getChannel() == null ? "EMAIL" : notification.getChannel().name(), Collectors.counting()));
        Map<String, Long> byCategory = notifications.stream()
                .collect(Collectors.groupingBy(notification -> normalize(notification.getCategory(), "GENERAL"), Collectors.counting()));

        return NotificationDtos.NotificationAnalyticsResponse.builder()
                .total(total)
                .sent(sent)
                .pending(pending)
                .failed(failed)
                .read(read)
                .archived(archived)
                .views(views)
                .clicks(clicks)
                .conversions(conversions)
                .deliveryRate(percent(sent, Math.max(total, 1)))
                .openRate(percent(views, Math.max(sent, 1)))
                .clickRate(percent(clicks, Math.max(views, 1)))
                .byChannel(byChannel)
                .byCategory(byCategory)
                .build();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processNotifications() {
        List<NotificationQueue> list = repository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");

        for (NotificationQueue n : list) {
            try {
                NotificationChannel channel = n.getChannel() == null ? NotificationChannel.EMAIL : n.getChannel();
                
                if (channel == NotificationChannel.EMAIL && isEmailConfigured()) {
                    emailService.send(n.getEmail(), n.getTitle(), n.getMessage());
                } else {
                    log.info("Simulated {} notification for user {}: {}", channel, n.getUserId(), n.getTitle());
                }

                n.setStatus("SENT");
                repository.save(n);
                analyticsService.recordSend(n.getCampaignId());
                log.info("Sent notification to: {}", n.getEmail());

                // Log successful channel delivery
                deliveryLogRepository.save(DeliveryLog.builder()
                        .notificationId(n.getId())
                        .channel(channel)
                        .status("DELIVERED")
                        .build());

            } catch (Exception e) {
                log.error("Failed to send notification to: {}", n.getEmail(), e);
                
                // Register in reliability retry queue
                if (reliabilityEngine != null) {
                    reliabilityEngine.registerForRetry(n.getId());
                }

                n.setRetryCount(n.getRetryCount() + 1);
                if (n.getRetryCount() >= 3) {
                    n.setStatus("FAILED");
                }
                repository.save(n);

                // Log failed channel delivery
                deliveryLogRepository.save(DeliveryLog.builder()
                        .notificationId(n.getId())
                        .channel(n.getChannel() == null ? NotificationChannel.EMAIL : n.getChannel())
                        .status("FAILED")
                        .build());
            }
        }
    }

    private boolean isEmailConfigured() {
        return emailEnabled;
    }

    private List<NotificationChannel> parseChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of(NotificationChannel.PUSH);
        }
        return channels.stream()
                .filter(Objects::nonNull)
                .map(channel -> NotificationChannel.valueOf(normalize(channel, "PUSH")))
                .distinct()
                .toList();
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String status(NotificationQueue notification) {
        return normalize(notification.getStatus(), "PENDING");
    }

    private boolean isRead(NotificationQueue notification) {
        return notification.getReadAt() != null || "READ".equalsIgnoreCase(status(notification));
    }

    private boolean isArchived(NotificationQueue notification) {
        return notification.getArchivedAt() != null || "ARCHIVED".equalsIgnoreCase(status(notification));
    }

    private boolean userHasRole(User user, String role) {
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(item -> item.getName() != null && item.getName().name().equalsIgnoreCase(normalizedRole));
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return Math.round((value * 10000.0) / total) / 100.0;
    }

    private boolean isCategoryDisabled(NotificationPreference preference, String category) {
        String disabledCategories = preference.getDisabledCategories();
        if (disabledCategories == null || disabledCategories.isBlank()) {
            return false;
        }
        return Arrays.stream(disabledCategories.split(","))
                .map(value -> normalize(value, ""))
                .anyMatch(disabledCategory -> disabledCategory.equals(category));
    }
}
