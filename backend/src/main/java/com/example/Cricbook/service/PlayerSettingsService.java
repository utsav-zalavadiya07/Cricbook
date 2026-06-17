package com.example.Cricbook.service;

import com.example.Cricbook.dto.PlayerSettingsRequest;
import com.example.Cricbook.dto.PlayerSettingsResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.NotificationChannel;
import com.example.Cricbook.enums.PlayerPosition;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlayerSettingsService {

    private final UserRepository userRepository;
    private final PlayerSettingsRepository playerSettingsRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final BookingPreferenceRepository bookingPreferenceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final SessionManagementService sessionManagementService;

    public PlayerSettingsService(UserRepository userRepository,
                                 PlayerSettingsRepository playerSettingsRepository,
                                 PlayerProfileRepository playerProfileRepository,
                                 BookingPreferenceRepository bookingPreferenceRepository,
                                 NotificationPreferenceRepository notificationPreferenceRepository,
                                 SessionManagementService sessionManagementService) {
        this.userRepository = userRepository;
        this.playerSettingsRepository = playerSettingsRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.bookingPreferenceRepository = bookingPreferenceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.sessionManagementService = sessionManagementService;
    }

    @Transactional(readOnly = true)
    public PlayerSettingsResponse getSettings(String loginId) {
        User user = findUser(loginId);
        PlayerSettings settings = findSettings(user);
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElse(null);
        BookingPreference bookingPreference = bookingPreferenceRepository.findByUserId(user.getId()).orElse(null);
        List<NotificationPreference> notificationPreferences = notificationPreferenceRepository.findByUserId(user.getId());

        return map(user, settings, profile, bookingPreference, notificationPreferences);
    }

    @Transactional
    public PlayerSettingsResponse updateSettings(String loginId, PlayerSettingsRequest request) {
        User user = findUser(loginId);
        PlayerSettings settings = playerSettingsRepository.findByUserId(user.getId()).orElseGet(() -> defaultSettings(user));
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElseGet(() -> PlayerProfile.builder()
                .user(user)
                .matchesPlayed(0)
                .wins(0)
                .mvpCount(0)
                .rating(0.0)
                .build());
        BookingPreference bookingPreference = bookingPreferenceRepository.findByUserId(user.getId()).orElseGet(() -> BookingPreference.builder().user(user).build());

        if (request != null) {
            applyProfile(user, profile, settings, request.getProfile());
            applySecurity(settings, request.getSecurity());
            applyAppearance(user, request.getAppearance());
            applyPrivacy(settings, request.getPrivacy());
            applyBooking(settings, bookingPreference, request.getBooking());
            applyMatchmaking(settings, request.getMatchmaking());
            applyCommunity(settings, request.getCommunity());
            applyPayments(settings, request.getPayments());
            applyLocalization(user, settings, request.getLocalization());
            applyData(settings, request.getData());
            applyNotifications(user.getId(), request.getNotifications());
        }

        userRepository.save(user);
        playerSettingsRepository.save(settings);
        playerProfileRepository.save(profile);
        bookingPreferenceRepository.save(bookingPreference);

        return map(user, settings, profile, bookingPreference, notificationPreferenceRepository.findByUserId(user.getId()));
    }

    @Transactional
    public PlayerSettingsResponse requestDataExport(String loginId) {
        User user = findUser(loginId);
        PlayerSettings settings = playerSettingsRepository.findByUserId(user.getId()).orElseGet(() -> defaultSettings(user));
        settings.setDownloadDataRequested(true);
        settings.setLastExportRequestedAt(LocalDateTime.now());
        playerSettingsRepository.save(settings);
        return map(user, settings,
                playerProfileRepository.findByUserId(user.getId()).orElse(null),
                bookingPreferenceRepository.findByUserId(user.getId()).orElse(null),
                notificationPreferenceRepository.findByUserId(user.getId()));
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private PlayerSettings findSettings(User user) {
        return playerSettingsRepository.findByUserId(user.getId()).orElseGet(() -> defaultSettings(user));
    }

    private PlayerSettings defaultSettings(User user) {
        return PlayerSettings.builder().user(user).build();
    }

    private void applyProfile(User user, PlayerProfile profile, PlayerSettings settings, PlayerSettingsRequest.ProfileSection request) {
        if (request == null) {
            return;
        }
        if (hasText(request.getDisplayName())) user.setName(request.getDisplayName().trim());
        if (hasText(request.getPhone())) user.setPhone(request.getPhone().trim());
        if (request.getBio() != null) profile.setBio(clean(request.getBio()));
        if (request.getPhotoUrl() != null) profile.setPhotoUrl(clean(request.getPhotoUrl()));
        if (request.getCity() != null) profile.setCity(clean(request.getCity()));
        if (request.getPosition() != null && hasText(request.getPosition())) {
            profile.setPosition(parsePosition(request.getPosition()));
        }
        if (request.getSkillLevel() != null) profile.setExperience(clean(request.getSkillLevel()));
        if (request.getVisibility() != null) settings.setProfileVisibility(normalizeChoice(request.getVisibility(), List.of("PUBLIC", "FRIENDS", "PRIVATE"), "PUBLIC"));
    }

    private void applySecurity(PlayerSettings settings, PlayerSettingsRequest.SecuritySection request) {
        if (request == null) return;
        if (request.getTwoFactorEnabled() != null) settings.setTwoFactorEnabled(request.getTwoFactorEnabled());
        if (request.getLoginMethod() != null) settings.setLoginMethod(normalizeChoice(request.getLoginMethod(), List.of("EMAIL_PHONE_PASSWORD", "OTP", "GOOGLE"), "EMAIL_PHONE_PASSWORD"));
    }

    private void applyAppearance(User user, PlayerSettingsRequest.AppearanceSection request) {
        if (request == null) return;
        if (request.getTheme() != null) user.setPreferredTheme(sanitizeTheme(request.getTheme()));
    }

    private void applyPrivacy(PlayerSettings settings, PlayerSettingsRequest.PrivacySection request) {
        if (request == null) return;
        if (request.getProfileVisibility() != null) settings.setProfileVisibility(normalizeChoice(request.getProfileVisibility(), List.of("PUBLIC", "FRIENDS", "PRIVATE"), "PUBLIC"));
        if (request.getShowTrustScore() != null) settings.setShowTrustScore(request.getShowTrustScore());
        if (request.getShowStats() != null) settings.setShowStats(request.getShowStats());
        if (request.getShowAvailability() != null) settings.setShowAvailability(request.getShowAvailability());
        if (request.getShowLocation() != null) settings.setShowLocation(request.getShowLocation());
    }

    private void applyBooking(PlayerSettings settings, BookingPreference preference, PlayerSettingsRequest.BookingSection request) {
        if (request == null) return;
        if (request.getPreferredDay() != null && hasText(request.getPreferredDay())) {
            try {
                preference.setPreferredDay(DayOfWeek.valueOf(request.getPreferredDay().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("Preferred day must be a valid weekday.");
            }
        }
        if (request.getPreferredTime() != null && hasText(request.getPreferredTime())) {
            try {
                preference.setPreferredTime(LocalTime.parse(request.getPreferredTime().trim()));
            } catch (RuntimeException ex) {
                throw ApiException.badRequest("Preferred time must use HH:mm format.");
            }
        }
        if (request.getPreferredDurationMinutes() != null) preference.setPreferredDurationMinutes(Math.max(30, request.getPreferredDurationMinutes()));
        if (request.getPreferredCity() != null) preference.setPreferredCity(clean(request.getPreferredCity()));
        if (request.getPreferredTeamId() != null) preference.setPreferredTeamId(request.getPreferredTeamId());
        if (request.getAutoWaitlistEnabled() != null) settings.setAutoWaitlistEnabled(request.getAutoWaitlistEnabled());
        if (request.getQuickRebookEnabled() != null) settings.setQuickRebookEnabled(request.getQuickRebookEnabled());
    }

    private void applyMatchmaking(PlayerSettings settings, PlayerSettingsRequest.MatchmakingSection request) {
        if (request == null) return;
        if (request.getCompetitionLevel() != null) settings.setCompetitionLevel(normalizeChoice(request.getCompetitionLevel(), List.of("CASUAL", "COMPETITIVE", "TOURNAMENT", "CORPORATE"), "CASUAL"));
        if (request.getPreferredSkillLevel() != null) settings.setPreferredSkillLevel(normalizeChoice(request.getPreferredSkillLevel(), List.of("ANY", "BEGINNER", "INTERMEDIATE", "ADVANCED", "PROFESSIONAL"), "ANY"));
        if (request.getTrustedOnly() != null) settings.setTrustedOnlyMatchmaking(request.getTrustedOnly());
        if (request.getNearbyOnly() != null) settings.setNearbyOnlyMatchmaking(request.getNearbyOnly());
    }

    private void applyCommunity(PlayerSettings settings, PlayerSettingsRequest.CommunitySection request) {
        if (request == null) return;
        if (request.getAllowTeamInvites() != null) settings.setAllowTeamInvites(request.getAllowTeamInvites());
        if (request.getAllowFriendRequests() != null) settings.setAllowFriendRequests(request.getAllowFriendRequests());
        if (request.getAllowMessages() != null) settings.setAllowMessages(request.getAllowMessages());
    }

    private void applyPayments(PlayerSettings settings, PlayerSettingsRequest.PaymentSection request) {
        if (request == null) return;
        if (request.getDefaultPaymentMethod() != null) settings.setDefaultPaymentMethod(clean(request.getDefaultPaymentMethod()));
        if (request.getSavedUpiId() != null) settings.setSavedUpiId(clean(request.getSavedUpiId()));
        if (request.getAutoPaySplits() != null) settings.setAutoPaySplits(request.getAutoPaySplits());
    }

    private void applyLocalization(User user, PlayerSettings settings, PlayerSettingsRequest.LocalizationSection request) {
        if (request == null) return;
        if (request.getLanguage() != null) user.setPreferredLanguage(sanitizeLanguage(request.getLanguage()));
        if (request.getRegionCity() != null) settings.setRegionCity(cleanOrDefault(request.getRegionCity(), "Ahmedabad"));
        if (request.getCurrency() != null) settings.setCurrency(normalizeChoice(request.getCurrency(), List.of("INR", "USD", "AED"), "INR"));
        if (request.getTimezone() != null) settings.setTimezone(cleanOrDefault(request.getTimezone(), "Asia/Kolkata"));
    }

    private void applyData(PlayerSettings settings, PlayerSettingsRequest.DataSection request) {
        if (request == null || request.getDownloadDataRequested() == null) return;
        settings.setDownloadDataRequested(request.getDownloadDataRequested());
        if (request.getDownloadDataRequested()) {
            settings.setLastExportRequestedAt(LocalDateTime.now());
        }
    }

    private void applyNotifications(Long userId, PlayerSettingsRequest.NotificationSection request) {
        if (request == null) return;
        String frequency = hasText(request.getFrequency()) ? request.getFrequency().trim().toUpperCase() : "INSTANT";
        updateNotification(userId, NotificationChannel.PUSH, request.getPush(), frequency);
        updateNotification(userId, NotificationChannel.EMAIL, request.getEmail(), frequency);
        updateNotification(userId, NotificationChannel.SMS, request.getSms(), frequency);
        updateNotification(userId, NotificationChannel.WHATSAPP, request.getWhatsapp(), frequency);
    }

    private void updateNotification(Long userId, NotificationChannel channel, Boolean enabled, String frequency) {
        if (enabled == null) return;
        NotificationPreference preference = notificationPreferenceRepository.findByUserIdAndChannel(userId, channel)
                .orElseGet(() -> NotificationPreference.builder().userId(userId).channel(channel).build());
        preference.setEnabled(enabled);
        preference.setFrequency(frequency);
        notificationPreferenceRepository.save(preference);
    }

    private PlayerSettingsResponse map(User user, PlayerSettings settings, PlayerProfile profile, BookingPreference bookingPreference, List<NotificationPreference> notificationPreferences) {
        Map<NotificationChannel, Boolean> channels = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannel channel : NotificationChannel.values()) {
            channels.put(channel, true);
        }
        String frequency = "INSTANT";
        for (NotificationPreference preference : notificationPreferences) {
            channels.put(preference.getChannel(), preference.isEnabled());
            frequency = preference.getFrequency();
        }

        return PlayerSettingsResponse.builder()
                .profile(PlayerSettingsResponse.ProfileSection.builder()
                        .userId(user.getId())
                        .displayName(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .bio(profile == null ? null : profile.getBio())
                        .photoUrl(profile == null ? null : profile.getPhotoUrl())
                        .city(profile == null ? null : profile.getCity())
                        .position(profile == null || profile.getPosition() == null ? null : profile.getPosition().name())
                        .skillLevel(profile == null ? null : profile.getExperience())
                        .visibility(settings.getProfileVisibility())
                        .completionPercentage(profileCompletion(user, profile))
                        .build())
                .security(PlayerSettingsResponse.SecuritySection.builder()
                        .twoFactorEnabled(settings.getTwoFactorEnabled())
                        .loginMethod(settings.getLoginMethod())
                        .emailVerified(true)
                        .phoneVerified(true)
                        .build())
                .notifications(PlayerSettingsResponse.NotificationSection.builder()
                        .push(channels.get(NotificationChannel.PUSH))
                        .email(channels.get(NotificationChannel.EMAIL))
                        .sms(channels.get(NotificationChannel.SMS))
                        .whatsapp(channels.get(NotificationChannel.WHATSAPP))
                        .frequency(frequency)
                        .channels(channels.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue)))
                        .build())
                .appearance(PlayerSettingsResponse.AppearanceSection.builder()
                        .theme(user.getPreferredTheme())
                        .accentColor("emerald")
                        .compactMode(false)
                        .reduceMotion(false)
                        .build())
                .privacy(PlayerSettingsResponse.PrivacySection.builder()
                        .profileVisibility(settings.getProfileVisibility())
                        .showTrustScore(settings.getShowTrustScore())
                        .showStats(settings.getShowStats())
                        .showAvailability(settings.getShowAvailability())
                        .showLocation(settings.getShowLocation())
                        .build())
                .booking(PlayerSettingsResponse.BookingSection.builder()
                        .preferredDay(bookingPreference == null || bookingPreference.getPreferredDay() == null ? null : bookingPreference.getPreferredDay().name())
                        .preferredTime(bookingPreference == null || bookingPreference.getPreferredTime() == null ? null : bookingPreference.getPreferredTime().toString())
                        .preferredDurationMinutes(bookingPreference == null ? null : bookingPreference.getPreferredDurationMinutes())
                        .preferredCity(bookingPreference == null ? null : bookingPreference.getPreferredCity())
                        .preferredTeamId(bookingPreference == null ? null : bookingPreference.getPreferredTeamId())
                        .autoWaitlistEnabled(settings.getAutoWaitlistEnabled())
                        .quickRebookEnabled(settings.getQuickRebookEnabled())
                        .build())
                .matchmaking(PlayerSettingsResponse.MatchmakingSection.builder()
                        .competitionLevel(settings.getCompetitionLevel())
                        .preferredSkillLevel(settings.getPreferredSkillLevel())
                        .trustedOnly(settings.getTrustedOnlyMatchmaking())
                        .nearbyOnly(settings.getNearbyOnlyMatchmaking())
                        .build())
                .community(PlayerSettingsResponse.CommunitySection.builder()
                        .allowTeamInvites(settings.getAllowTeamInvites())
                        .allowFriendRequests(settings.getAllowFriendRequests())
                        .allowMessages(settings.getAllowMessages())
                        .build())
                .payments(PlayerSettingsResponse.PaymentSection.builder()
                        .defaultPaymentMethod(settings.getDefaultPaymentMethod())
                        .savedUpiId(settings.getSavedUpiId())
                        .autoPaySplits(settings.getAutoPaySplits())
                        .build())
                .localization(PlayerSettingsResponse.LocalizationSection.builder()
                        .language(user.getPreferredLanguage())
                        .regionCity(settings.getRegionCity())
                        .currency(settings.getCurrency())
                        .timezone(settings.getTimezone())
                        .build())
                .devices(PlayerSettingsResponse.DevicesSection.builder()
                        .activeSessions(sessionManagementService.activeSessionCount(user))
                        .currentDevice(sessionManagementService.currentDevice(user))
                        .lastActivity(sessionManagementService.latestActivity(user))
                        .build())
                .data(PlayerSettingsResponse.DataSection.builder()
                        .downloadDataRequested(settings.getDownloadDataRequested())
                        .lastExportRequestedAt(settings.getLastExportRequestedAt() == null ? null : settings.getLastExportRequestedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .exportFormats(List.of("JSON", "CSV"))
                        .build())
                .smartSuggestions(suggestions(settings, user, bookingPreference))
                .build();
    }

    private List<String> suggestions(PlayerSettings settings, User user, BookingPreference bookingPreference) {
        if (!settings.getShowLocation()) {
            return List.of("Enable location visibility for nearby match recommendations.", "Review notification channels to avoid missing booking updates.");
        }
        if (bookingPreference == null || bookingPreference.getPreferredTime() == null) {
            return List.of("Set your preferred playing time to improve rebook suggestions.");
        }
        return List.of("Your settings are ready for personalized booking, matchmaking, and team coordination.");
    }

    private String sanitizeTheme(String theme) {
        String normalized = cleanOrDefault(theme, "dark").toLowerCase();
        if (!List.of("light", "dark", "system").contains(normalized)) {
            throw ApiException.badRequest("Theme must be light, dark, or system.");
        }
        return normalized;
    }

    private String sanitizeLanguage(String language) {
        String normalized = cleanOrDefault(language, "en").toLowerCase();
        if (!List.of("en", "hi", "gu", "mr", "bn", "ta", "te", "kn", "ml", "pa").contains(normalized)) {
            throw ApiException.badRequest("Language is not supported yet.");
        }
        return normalized;
    }

    private PlayerPosition parsePosition(String value) {
        String normalized = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if ("BATSMAN".equals(normalized) || "BATTER".equals(normalized)) {
            normalized = "TOP_ORDER";
        }
        try {
            return PlayerPosition.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Playing role must be a valid cricket role.");
        }
    }

    private String normalizeChoice(String value, List<String> allowed, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("Invalid value: " + value + ".");
        }
        return normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isEmpty() ? fallback : cleaned;
    }

    private int profileCompletion(User user, PlayerProfile profile) {
        int score = 0;
        if (hasText(profile == null ? null : profile.getPhotoUrl())) score += 20;
        if (hasText(profile == null ? null : profile.getBio())) score += 20;
        if (hasText(profile == null ? null : profile.getCity())) score += 20;
        if (hasText(user.getEmail())) score += 20;
        if (hasText(user.getPhone())) score += 20;
        return score;
    }
}
