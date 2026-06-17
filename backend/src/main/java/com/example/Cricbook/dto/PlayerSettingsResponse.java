package com.example.Cricbook.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PlayerSettingsResponse {
    private ProfileSection profile;
    private SecuritySection security;
    private NotificationSection notifications;
    private AppearanceSection appearance;
    private PrivacySection privacy;
    private BookingSection booking;
    private MatchmakingSection matchmaking;
    private CommunitySection community;
    private PaymentSection payments;
    private LocalizationSection localization;
    private DevicesSection devices;
    private DataSection data;
    private List<String> smartSuggestions;

    @Data
    @Builder
    public static class ProfileSection {
        private Long userId;
        private String displayName;
        private String email;
        private String phone;
        private String bio;
        private String photoUrl;
        private String city;
        private String position;
        private String skillLevel;
        private String visibility;
        private Integer completionPercentage;
    }

    @Data
    @Builder
    public static class SecuritySection {
        private Boolean twoFactorEnabled;
        private String loginMethod;
        private Boolean emailVerified;
        private Boolean phoneVerified;
    }

    @Data
    @Builder
    public static class NotificationSection {
        private Boolean push;
        private Boolean email;
        private Boolean sms;
        private Boolean whatsapp;
        private String frequency;
        private Map<String, Boolean> channels;
    }

    @Data
    @Builder
    public static class AppearanceSection {
        private String theme;
        private String accentColor;
        private Boolean compactMode;
        private Boolean reduceMotion;
    }

    @Data
    @Builder
    public static class PrivacySection {
        private String profileVisibility;
        private Boolean showTrustScore;
        private Boolean showStats;
        private Boolean showAvailability;
        private Boolean showLocation;
    }

    @Data
    @Builder
    public static class BookingSection {
        private String preferredDay;
        private String preferredTime;
        private Integer preferredDurationMinutes;
        private String preferredCity;
        private Long preferredTeamId;
        private Boolean autoWaitlistEnabled;
        private Boolean quickRebookEnabled;
    }

    @Data
    @Builder
    public static class MatchmakingSection {
        private String competitionLevel;
        private String preferredSkillLevel;
        private Boolean trustedOnly;
        private Boolean nearbyOnly;
    }

    @Data
    @Builder
    public static class CommunitySection {
        private Boolean allowTeamInvites;
        private Boolean allowFriendRequests;
        private Boolean allowMessages;
    }

    @Data
    @Builder
    public static class PaymentSection {
        private String defaultPaymentMethod;
        private String savedUpiId;
        private Boolean autoPaySplits;
    }

    @Data
    @Builder
    public static class LocalizationSection {
        private String language;
        private String regionCity;
        private String currency;
        private String timezone;
    }

    @Data
    @Builder
    public static class DevicesSection {
        private Integer activeSessions;
        private String currentDevice;
        private String lastActivity;
    }

    @Data
    @Builder
    public static class DataSection {
        private Boolean downloadDataRequested;
        private String lastExportRequestedAt;
        private List<String> exportFormats;
    }
}
