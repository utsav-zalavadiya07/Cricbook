package com.example.Cricbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "owner_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "support_phone")
    private String supportPhone;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "business_logo_url")
    private String businessLogoUrl;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "business_type", nullable = false)
    @Builder.Default
    private String businessType = "SINGLE_TURF";

    @Column(name = "operating_hours")
    private String operatingHours;

    @Column(name = "default_slot_duration_minutes", nullable = false)
    @Builder.Default
    private Integer defaultSlotDurationMinutes = 60;

    @Column(name = "opening_time", nullable = false)
    @Builder.Default
    private String openingTime = "06:00";

    @Column(name = "closing_time", nullable = false)
    @Builder.Default
    private String closingTime = "23:00";

    @Column(name = "indoor_outdoor", nullable = false)
    @Builder.Default
    private String indoorOutdoor = "OUTDOOR";

    @Column(name = "amenities", columnDefinition = "TEXT")
    private String amenities;

    @Column(name = "advance_booking_days", nullable = false)
    @Builder.Default
    private Integer advanceBookingDays = 14;

    @Column(name = "minimum_booking_minutes", nullable = false)
    @Builder.Default
    private Integer minimumBookingMinutes = 60;

    @Column(name = "allow_instant_booking", nullable = false)
    @Builder.Default
    private Boolean allowInstantBooking = Boolean.TRUE;

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private Boolean approvalRequired = Boolean.FALSE;

    @Column(name = "trusted_players_only", nullable = false)
    @Builder.Default
    private Boolean trustedPlayersOnly = Boolean.FALSE;

    @Column(name = "cancellation_policy", nullable = false)
    @Builder.Default
    private String cancellationPolicy = "FLEXIBLE";

    @Column(name = "dynamic_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean dynamicPricingEnabled = Boolean.FALSE;

    @Column(name = "weekend_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean weekendPricingEnabled = Boolean.TRUE;

    @Column(name = "peak_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean peakPricingEnabled = Boolean.TRUE;

    @Column(name = "pricing_mode", nullable = false)
    @Builder.Default
    private String pricingMode = "MANUAL";

    @Column(name = "peak_multiplier", nullable = false)
    @Builder.Default
    private Double peakMultiplier = 1.20d;

    @Column(name = "weekend_multiplier", nullable = false)
    @Builder.Default
    private Double weekendMultiplier = 1.15d;

    @Column(name = "staff_invite_email")
    private String staffInviteEmail;

    @Column(name = "staff_role", nullable = false)
    @Builder.Default
    private String staffRole = "MANAGER";

    @Column(name = "staff_permissions", columnDefinition = "TEXT")
    private String staffPermissions;

    @Column(name = "promotion_whatsapp_enabled", nullable = false)
    @Builder.Default
    private Boolean promotionWhatsappEnabled = Boolean.FALSE;

    @Column(name = "reminder_lead_minutes", nullable = false)
    @Builder.Default
    private Integer reminderLeadMinutes = 120;

    @Column(name = "partial_payment_enabled", nullable = false)
    @Builder.Default
    private Boolean partialPaymentEnabled = Boolean.TRUE;

    @Column(name = "minimum_payment_percent", nullable = false)
    @Builder.Default
    private Integer minimumPaymentPercent = 50;

    @Column(name = "refund_mode", nullable = false)
    @Builder.Default
    private String refundMode = "WALLET_CREDIT";

    @Column(name = "rain_refund_allowed", nullable = false)
    @Builder.Default
    private Boolean rainRefundAllowed = Boolean.TRUE;

    @Column(name = "free_weather_reschedule", nullable = false)
    @Builder.Default
    private Boolean freeWeatherReschedule = Boolean.TRUE;

    @Column(name = "weather_risk_alerts", nullable = false)
    @Builder.Default
    private Boolean weatherRiskAlerts = Boolean.TRUE;

    @Column(name = "rain_threshold_percent", nullable = false)
    @Builder.Default
    private Integer rainThresholdPercent = 70;

    @Column(name = "resale_enabled", nullable = false)
    @Builder.Default
    private Boolean resaleEnabled = Boolean.TRUE;

    @Column(name = "transfer_enabled", nullable = false)
    @Builder.Default
    private Boolean transferEnabled = Boolean.TRUE;

    @Column(name = "max_resale_markup_percent", nullable = false)
    @Builder.Default
    private Integer maxResaleMarkupPercent = 0;

    @Column(name = "restrict_suspicious_accounts", nullable = false)
    @Builder.Default
    private Boolean restrictSuspiciousAccounts = Boolean.TRUE;

    @Column(name = "allow_public_tournaments", nullable = false)
    @Builder.Default
    private Boolean allowPublicTournaments = Boolean.TRUE;

    @Column(name = "community_visibility", nullable = false)
    @Builder.Default
    private String communityVisibility = "PUBLIC";

    @Column(name = "team_registration_rule", nullable = false)
    @Builder.Default
    private String teamRegistrationRule = "APPROVAL_REQUIRED";

    @Column(name = "brand_primary_color", nullable = false)
    @Builder.Default
    private String brandPrimaryColor = "#0EA5E9";

    @Column(name = "brand_theme", nullable = false)
    @Builder.Default
    private String brandTheme = "system";

    @Column(name = "compact_dashboard", nullable = false)
    @Builder.Default
    private Boolean compactDashboard = Boolean.FALSE;

    @Column(name = "two_factor_enabled", nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = Boolean.FALSE;

    @Column(name = "session_timeout_minutes", nullable = false)
    @Builder.Default
    private Integer sessionTimeoutMinutes = 60;

    @Column(name = "ip_restrictions")
    private String ipRestrictions;

    @Column(name = "auto_pricing_enabled", nullable = false)
    @Builder.Default
    private Boolean autoPricingEnabled = Boolean.FALSE;

    @Column(name = "auto_reschedule_enabled", nullable = false)
    @Builder.Default
    private Boolean autoRescheduleEnabled = Boolean.TRUE;

    @Column(name = "ai_coach_enabled", nullable = false)
    @Builder.Default
    private Boolean aiCoachEnabled = Boolean.TRUE;

    @Column(name = "smart_promotions_enabled", nullable = false)
    @Builder.Default
    private Boolean smartPromotionsEnabled = Boolean.TRUE;

    @Column(name = "revenue_export_enabled", nullable = false)
    @Builder.Default
    private Boolean revenueExportEnabled = Boolean.TRUE;

    @Column(name = "gst_reports_enabled", nullable = false)
    @Builder.Default
    private Boolean gstReportsEnabled = Boolean.TRUE;

    @Column(name = "export_format", nullable = false)
    @Builder.Default
    private String exportFormat = "CSV";

    @Column(name = "data_export_requested", nullable = false)
    @Builder.Default
    private Boolean dataExportRequested = Boolean.FALSE;

    @Column(name = "last_export_requested_at")
    private LocalDateTime lastExportRequestedAt;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "settlement_schedule", nullable = false)
    @Builder.Default
    private String settlementSchedule = "WEEKLY";

    @Column(name = "booking_alerts", nullable = false)
    @Builder.Default
    private Boolean bookingAlerts = Boolean.TRUE;

    @Column(name = "payment_alerts", nullable = false)
    @Builder.Default
    private Boolean paymentAlerts = Boolean.TRUE;

    @Column(name = "review_alerts", nullable = false)
    @Builder.Default
    private Boolean reviewAlerts = Boolean.TRUE;

    @Column(name = "marketing_alerts", nullable = false)
    @Builder.Default
    private Boolean marketingAlerts = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
