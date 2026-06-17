package com.example.Cricbook.dto;

import lombok.Data;

@Data
public class PlayerSettingsRequest {
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
    private DataSection data;

    @Data
    public static class ProfileSection {
        private String displayName;
        private String phone;
        private String bio;
        private String photoUrl;
        private String city;
        private String position;
        private String skillLevel;
        private String visibility;
    }

    @Data
    public static class SecuritySection {
        private Boolean twoFactorEnabled;
        private String loginMethod;
    }

    @Data
    public static class NotificationSection {
        private Boolean push;
        private Boolean email;
        private Boolean sms;
        private Boolean whatsapp;
        private String frequency;
    }

    @Data
    public static class AppearanceSection {
        private String theme;
        private String accentColor;
        private Boolean compactMode;
        private Boolean reduceMotion;
    }

    @Data
    public static class PrivacySection {
        private String profileVisibility;
        private Boolean showTrustScore;
        private Boolean showStats;
        private Boolean showAvailability;
        private Boolean showLocation;
    }

    @Data
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
    public static class MatchmakingSection {
        private String competitionLevel;
        private String preferredSkillLevel;
        private Boolean trustedOnly;
        private Boolean nearbyOnly;
    }

    @Data
    public static class CommunitySection {
        private Boolean allowTeamInvites;
        private Boolean allowFriendRequests;
        private Boolean allowMessages;
    }

    @Data
    public static class PaymentSection {
        private String defaultPaymentMethod;
        private String savedUpiId;
        private Boolean autoPaySplits;
    }

    @Data
    public static class LocalizationSection {
        private String language;
        private String regionCity;
        private String currency;
        private String timezone;
    }

    @Data
    public static class DataSection {
        private Boolean downloadDataRequested;
    }
}
