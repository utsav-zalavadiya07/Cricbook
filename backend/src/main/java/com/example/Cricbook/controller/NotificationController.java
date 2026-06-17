package com.example.Cricbook.controller;

import com.example.Cricbook.dto.NotificationDtos.BulkNotificationRequest;
import com.example.Cricbook.dto.NotificationDtos.BroadcastRequest;
import com.example.Cricbook.dto.NotificationDtos.BroadcastResponse;
import com.example.Cricbook.dto.NotificationDtos.EventCatalogResponse;
import com.example.Cricbook.dto.NotificationDtos.NotificationAnalyticsResponse;
import com.example.Cricbook.dto.NotificationDtos.NotificationResponse;
import com.example.Cricbook.dto.NotificationDtos.NotificationSummaryResponse;
import com.example.Cricbook.dto.NotificationDtos.PreferenceResponse;
import com.example.Cricbook.dto.NotificationDtos.PreferenceUpdateRequest;
import com.example.Cricbook.dto.NotificationDtos.TemplatePreviewRequest;
import com.example.Cricbook.dto.NotificationDtos.TemplatePreviewResponse;
import com.example.Cricbook.entity.Announcement;
import com.example.Cricbook.entity.NotificationPreference;
import com.example.Cricbook.entity.NotificationQueue;
import com.example.Cricbook.entity.NotificationTemplate;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.AnnouncementRepository;
import com.example.Cricbook.repository.NotificationPreferenceRepository;
import com.example.Cricbook.repository.NotificationRepository;
import com.example.Cricbook.repository.NotificationTemplateRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.EventBusService;
import com.example.Cricbook.service.NotificationAnalyticsService;
import com.example.Cricbook.service.NotificationService;
import com.example.Cricbook.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Set<String> VALID_FREQUENCIES = Set.of("INSTANT", "DAILY", "WEEKLY");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationAnalyticsService analyticsService;
    private final EventBusService eventBusService;
    private final NotificationService notificationService;
    private final TemplateService templateService;
    private final NotificationTemplateRepository templateRepository;
    private final AnnouncementRepository announcementRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        User user = findUser(userDetails.getUsername());
        List<NotificationQueue> notifications = applyFilters(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()),
                folder,
                search,
                channel,
                type,
                priority
        );
        notifications = paginate(notifications, page, size);

        return ResponseEntity.ok(notifications.stream().map(this::toResponse).toList());
    }

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryResponse> getSummary(@AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        List<NotificationQueue> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        Map<String, Long> byChannel = notifications.stream()
                .collect(Collectors.groupingBy(n -> n.getChannel() == null ? "EMAIL" : n.getChannel().name(), Collectors.counting()));
        Map<String, Long> byCategory = notifications.stream()
                .collect(Collectors.groupingBy(n -> normalize(n.getCategory(), "GENERAL"), Collectors.counting()));

        long archived = notifications.stream().filter(this::isArchived).count();
        long read = notifications.stream().filter(this::isRead).count();
        long unread = notifications.stream().filter(n -> !isArchived(n) && !isRead(n)).count();
        long pending = notifications.stream().filter(n -> "PENDING".equalsIgnoreCase(status(n))).count();
        long failed = notifications.stream().filter(n -> "FAILED".equalsIgnoreCase(status(n))).count();

        return ResponseEntity.ok(NotificationSummaryResponse.builder()
                .total(notifications.size())
                .unread(unread)
                .read(read)
                .archived(archived)
                .pending(pending)
                .failed(failed)
                .byChannel(byChannel)
                .byCategory(byCategory)
                .build());
    }

    @Transactional
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        int updated = notificationRepository.markAsRead(id, user.getId());
        if (updated == 0) {
            throw ApiException.notFound("Notification was not found.");
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PatchMapping("/bulk/read")
    public ResponseEntity<Void> markManyAsRead(
            @RequestBody BulkNotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        List<Long> ids = sanitizeIds(request);
        if (!ids.isEmpty()) {
            notificationRepository.markManyAsRead(user.getId(), ids);
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        notificationRepository.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archiveNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        int updated = notificationRepository.archive(id, user.getId());
        if (updated == 0) {
            throw ApiException.notFound("Notification was not found.");
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PatchMapping("/bulk/archive")
    public ResponseEntity<Void> archiveMany(
            @RequestBody BulkNotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        List<Long> ids = sanitizeIds(request);
        if (!ids.isEmpty()) {
            notificationRepository.archiveMany(user.getId(), ids);
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        int deleted = notificationRepository.deleteByIdAndUserId(id, user.getId());
        if (deleted == 0) {
            throw ApiException.notFound("Notification was not found.");
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> deleteMany(
            @RequestBody BulkNotificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        List<Long> ids = sanitizeIds(request);
        if (!ids.isEmpty()) {
            notificationRepository.deleteManyByUserId(user.getId(), ids);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<PreferenceResponse>> getPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        User user = findUser(userDetails.getUsername());
        List<NotificationPreference> prefs = preferenceRepository.findByUserId(user.getId());

        if (prefs.isEmpty()) {
            prefs = new ArrayList<>();
            for (NotificationChannel channel : NotificationChannel.values()) {
                prefs.add(preferenceRepository.save(NotificationPreference.builder()
                        .userId(user.getId())
                        .channel(channel)
                        .enabled(true)
                        .frequency("INSTANT")
                        .disabledCategories("")
                        .build()));
            }
        }
        return ResponseEntity.ok(prefs.stream().map(this::toPreferenceResponse).toList());
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<Announcement>> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementRepository.findByStatusInOrderByCreatedAtDesc(List.of("ACTIVE", "PUBLISHED")));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('NOTIFICATION_ANALYTICS') or hasAuthority('CAMPAIGN_VIEW')")
    public ResponseEntity<NotificationAnalyticsResponse> getPlatformAnalytics() {
        return ResponseEntity.ok(notificationService.platformAnalytics());
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('NOTIFICATION_SEND') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('PUSH_SEND') or hasAuthority('EMAIL_SEND')")
    public ResponseEntity<BroadcastResponse> broadcast(@RequestBody BroadcastRequest request) {
        return ResponseEntity.ok(notificationService.broadcast(request));
    }

    @PostMapping("/templates/preview")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('NOTIFICATION_TEMPLATE_EDIT') or hasAuthority('CMS_EDIT') or hasAuthority('EMAIL_SEND') or hasAuthority('PUSH_SEND')")
    public ResponseEntity<TemplatePreviewResponse> previewTemplate(@RequestBody TemplatePreviewRequest request) {
        NotificationTemplate template = null;
        if (request != null && request.getTemplateName() != null && !request.getTemplateName().isBlank()) {
            template = templateRepository.findByName(request.getTemplateName()).orElse(null);
        }
        String subject = template == null ? request == null ? "" : defaultString(request.getSubject(), "") : defaultString(template.getSubject(), "");
        String body = template == null ? request == null ? "" : defaultString(request.getBody(), "") : template.getBody();
        NotificationChannel channel = template == null ? parseOptionalChannel(request == null ? null : request.getChannel()) : template.getChannel();
        Map<String, String> variables = request == null || request.getVariables() == null ? Map.of() : request.getVariables();
        return ResponseEntity.ok(TemplatePreviewResponse.builder()
                .subject(templateService.render(subject, variables))
                .body(templateService.render(body, variables))
                .channel(channel)
                .variables(variables)
                .build());
    }

    @GetMapping("/events/catalog")
    public ResponseEntity<List<EventCatalogResponse>> eventCatalog() {
        return ResponseEntity.ok(List.of(
                event("BOOKING_CREATED", "BOOKING", "/bookings", List.of("EMAIL", "WHATSAPP", "PUSH"), List.of("userId", "bookingId", "date")),
                event("PAYMENT_SUCCESS", "PAYMENT", "/wallet", List.of("EMAIL", "PUSH"), List.of("userId", "amount", "transactionId")),
                event("REFUND_COMPLETED", "PAYMENT", "/wallet", List.of("PUSH", "EMAIL"), List.of("userId", "amount", "refundId")),
                event("TEAM_INVITE", "TEAM", "/teams", List.of("PUSH", "EMAIL"), List.of("userId", "teamName")),
                event("MATCH_STARTED", "MATCH", "/live-scoring", List.of("PUSH"), List.of("matchId", "teamA", "teamB")),
                event("TOURNAMENT_UPDATE", "TOURNAMENT", "/tournaments", List.of("PUSH", "EMAIL"), List.of("title", "message", "tournamentId")),
                event("REVIEW_RECEIVED", "REVIEW", "/owner/reviews", List.of("WHATSAPP", "PUSH"), List.of("ownerId", "turfName", "stars")),
                event("SOCIAL_MENTION", "SOCIAL", "/feed", List.of("PUSH"), List.of("userId", "actor"))
        ));
    }

    @Transactional
    @PutMapping("/preferences")
    public ResponseEntity<PreferenceResponse> updatePreference(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PreferenceUpdateRequest request) {

        User user = findUser(userDetails.getUsername());
        NotificationChannel channel = parseChannel(request.getChannel());
        String frequency = normalize(request.getFrequency(), "INSTANT");
        if (!VALID_FREQUENCIES.contains(frequency)) {
            throw ApiException.badRequest("Notification frequency must be INSTANT, DAILY, or WEEKLY.");
        }

        NotificationPreference pref = preferenceRepository.findByUserIdAndChannel(user.getId(), channel)
                .orElseGet(() -> NotificationPreference.builder()
                        .userId(user.getId())
                        .channel(channel)
                        .enabled(true)
                        .frequency("INSTANT")
                        .disabledCategories("")
                        .build());

        if (request.getEnabled() != null) {
            pref.setEnabled(request.getEnabled());
        }
        pref.setFrequency(frequency);
        applyCategoryPreference(pref, request.getCategory(), request.getCategoryEnabled());

        return ResponseEntity.ok(toPreferenceResponse(preferenceRepository.save(pref)));
    }

    @Transactional
    @PatchMapping("/{id}/track")
    public ResponseEntity<Void> trackMetric(
            @PathVariable Long id,
            @RequestParam String action,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = findUser(userDetails.getUsername());
        NotificationQueue notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> ApiException.notFound("Notification was not found."));

        if ("view".equalsIgnoreCase(action)) {
            analyticsService.recordView(notification.getCampaignId(), notification.getId());
        } else if ("click".equalsIgnoreCase(action)) {
            analyticsService.recordClick(notification.getCampaignId(), notification.getId());
        } else {
            throw ApiException.badRequest("Tracking action must be view or click.");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trigger-event")
    public ResponseEntity<Void> triggerEvent(@RequestBody Map<String, Object> request) {
        Object eventName = request.get("event");
        if (eventName == null || eventName.toString().isBlank()) {
            throw ApiException.badRequest("Event parameter is required.");
        }

        Map<String, String> payload = request.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
        eventBusService.publishEvent(eventName.toString(), payload);
        return ResponseEntity.ok().build();
    }

    private List<NotificationQueue> applyFilters(List<NotificationQueue> notifications, String folder, String search, String channel, String type, String priority) {
        LocalDateTime now = LocalDateTime.now();
        String resolvedFolder = normalize(folder, "ALL");
        String resolvedChannel = normalize(channel, "");
        String resolvedType = normalize(type, "");
        String resolvedPriority = normalize(priority, "");

        List<NotificationQueue> filtered = notifications.stream()
                .filter(n -> switch (resolvedFolder) {
                    case "TODAY" -> n.getCreatedAt() != null
                            && n.getCreatedAt().isAfter(now.toLocalDate().atStartOfDay())
                            && !isArchived(n);
                    case "THIS_WEEK" -> n.getCreatedAt() != null
                            && n.getCreatedAt().isAfter(now.minusDays(7))
                            && !isArchived(n);
                    case "ARCHIVE", "ARCHIVED" -> isArchived(n);
                    case "UNREAD" -> !isArchived(n) && !isRead(n);
                    case "READ" -> !isArchived(n) && isRead(n);
                    case "ALL_WITH_ARCHIVE" -> true;
                    default -> !isArchived(n);
                })
                .filter(n -> resolvedChannel.isBlank() || (n.getChannel() == null ? "EMAIL" : n.getChannel().name()).equals(resolvedChannel))
                .filter(n -> resolvedType.isBlank() || normalize(n.getCategory(), "GENERAL").equals(resolvedType))
                .filter(n -> resolvedPriority.isBlank() || normalize(n.getPriority(), "NORMAL").equals(resolvedPriority))
                .toList();

        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return filtered;
        }
        if (query.length() > 120) {
            throw ApiException.badRequest("Notification search must be 120 characters or fewer.");
        }
        return filtered.stream()
                .filter(n -> contains(n.getTitle(), query)
                        || contains(n.getMessage(), query)
                        || contains(n.getCategory(), query)
                        || contains(n.getPriority(), query))
                .toList();
    }

    private List<NotificationQueue> paginate(List<NotificationQueue> notifications, Integer page, Integer size) {
        int resolvedSize = size == null ? notifications.size() : Math.max(1, Math.min(size, 100));
        if (resolvedSize == 0 || notifications.isEmpty()) {
            return notifications;
        }
        int resolvedPage = Math.max(page == null ? 0 : page, 0);
        int fromIndex = resolvedPage * resolvedSize;
        if (fromIndex >= notifications.size()) {
            return List.of();
        }
        return notifications.subList(fromIndex, Math.min(fromIndex + resolvedSize, notifications.size()));
    }

    private NotificationResponse toResponse(NotificationQueue notification) {
        String currentStatus = status(notification);
        boolean read = isRead(notification);
        boolean archived = isArchived(notification);
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(currentStatus)
                .deliveryStatus(read || archived ? "SENT" : currentStatus)
                .read(read)
                .archived(archived)
                .channel(notification.getChannel() == null ? NotificationChannel.EMAIL : notification.getChannel())
                .category(normalize(notification.getCategory(), "GENERAL"))
                .type(normalize(notification.getCategory(), "GENERAL"))
                .priority(normalize(notification.getPriority(), "NORMAL"))
                .campaignId(notification.getCampaignId())
                .deepLink(notification.getDeepLink())
                .retryCount(notification.getRetryCount())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .archivedAt(notification.getArchivedAt())
                .build();
    }

    private PreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return PreferenceResponse.builder()
                .id(preference.getId())
                .channel(preference.getChannel())
                .enabled(preference.isEnabled())
                .frequency(preference.getFrequency())
                .disabledCategories(parseCategories(preference.getDisabledCategories()).stream().toList())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    private void applyCategoryPreference(NotificationPreference preference, String category, Boolean categoryEnabled) {
        if (category == null || category.isBlank() || categoryEnabled == null) {
            return;
        }
        String normalizedCategory = normalize(category, "GENERAL");
        Set<String> disabled = parseCategories(preference.getDisabledCategories());
        if (categoryEnabled) {
            disabled.remove(normalizedCategory);
        } else {
            disabled.add(normalizedCategory);
        }
        preference.setDisabledCategories(String.join(",", disabled));
    }

    private Set<String> parseCategories(String categories) {
        if (categories == null || categories.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(categories.split(","))
                .map(category -> normalize(category, ""))
                .filter(category -> !category.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> sanitizeIds(BulkNotificationRequest request) {
        if (request == null || request.getIds() == null) {
            return List.of();
        }
        return request.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(100)
                .toList();
    }

    private NotificationChannel parseChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw ApiException.badRequest("Notification channel is required.");
        }
        try {
            return NotificationChannel.valueOf(normalize(channel, ""));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Notification channel must be EMAIL, PUSH, SMS, or WHATSAPP.");
        }
    }

    private NotificationChannel parseOptionalChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return NotificationChannel.EMAIL;
        }
        return parseChannel(channel);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
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

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private EventCatalogResponse event(String name, String category, String deepLink, List<String> channels, List<String> requiredPayload) {
        return EventCatalogResponse.builder()
                .event(name)
                .category(category)
                .deepLink(deepLink)
                .recommendedChannels(channels)
                .requiredPayload(requiredPayload)
                .build();
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }
}
