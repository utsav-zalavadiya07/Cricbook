package com.example.Cricbook.service;

import com.example.Cricbook.dto.OwnerSettingsRequest;
import com.example.Cricbook.dto.OwnerSettingsResponse;
import com.example.Cricbook.entity.OwnerSettings;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.OwnerSettingsRepository;
import com.example.Cricbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OwnerSettingsService {

    private static final List<String> BUSINESS_TYPES = List.of("SINGLE_TURF", "MULTI_TURF", "FRANCHISE");
    private static final List<String> ENVIRONMENTS = List.of("INDOOR", "OUTDOOR", "SEMI_INDOOR");
    private static final List<String> CANCELLATION_POLICIES = List.of("FLEXIBLE", "MODERATE", "STRICT");
    private static final List<String> PRICING_MODES = List.of("MANUAL", "AI_SUGGESTED", "AUTOMATIC");
    private static final List<String> STAFF_ROLES = List.of("MANAGER", "RECEPTIONIST", "GROUND_STAFF", "FINANCE_STAFF", "TOURNAMENT_ADMIN");
    private static final List<String> REFUND_MODES = List.of("FULL", "PARTIAL", "WALLET_CREDIT", "RESCHEDULE_ONLY");
    private static final List<String> VISIBILITY_OPTIONS = List.of("PUBLIC", "INVITE_ONLY", "PRIVATE");
    private static final List<String> REGISTRATION_RULES = List.of("OPEN", "APPROVAL_REQUIRED", "INVITE_ONLY");
    private static final List<String> THEMES = List.of("light", "dark", "system");
    private static final List<String> EXPORT_FORMATS = List.of("CSV", "EXCEL", "PDF");

    private final UserRepository userRepository;
    private final OwnerSettingsRepository ownerSettingsRepository;

    @Transactional(readOnly = true)
    public OwnerSettingsResponse getSettings(String loginId) {
        User owner = findOwner(loginId);
        OwnerSettings settings = withDefaults(ownerSettingsRepository.findByOwnerId(owner.getId()).orElseGet(() -> defaults(owner)), owner);
        return map(owner, settings);
    }

    @Transactional
    public OwnerSettingsResponse updateSettings(String loginId, OwnerSettingsRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Owner settings are required.");
        }

        User owner = findOwner(loginId);
        OwnerSettings settings = withDefaults(ownerSettingsRepository.findByOwnerId(owner.getId()).orElseGet(() -> defaults(owner)), owner);

        applyBusiness(settings, request);
        applyTurfConfiguration(settings, request);
        applyBookingPolicy(settings, request);
        applyPricing(settings, request);
        applyStaff(settings, request);
        applyPayments(settings, request);
        applyNotifications(settings, request);
        applyWeather(settings, request);
        applyMarketplace(settings, request);
        applyCommunity(settings, request);
        applyBranding(settings, request);
        applySecurity(settings, request);
        applyAutomation(settings, request);
        applyReports(settings, request);

        validate(settings);
        return map(owner, ownerSettingsRepository.save(settings));
    }

    @Transactional
    public OwnerSettingsResponse requestExport(String loginId) {
        User owner = findOwner(loginId);
        OwnerSettings settings = withDefaults(ownerSettingsRepository.findByOwnerId(owner.getId()).orElseGet(() -> defaults(owner)), owner);
        settings.setDataExportRequested(Boolean.TRUE);
        settings.setLastExportRequestedAt(LocalDateTime.now());
        return map(owner, ownerSettingsRepository.save(settings));
    }

    private void applyBusiness(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getBusinessName() != null) settings.setBusinessName(clean(request.getBusinessName()));
        if (request.getSupportPhone() != null) settings.setSupportPhone(clean(request.getSupportPhone()));
        if (request.getBusinessAddress() != null) settings.setBusinessAddress(clean(request.getBusinessAddress()));
        if (request.getGstNumber() != null) settings.setGstNumber(cleanUpper(request.getGstNumber()));
        if (request.getBusinessLogoUrl() != null) settings.setBusinessLogoUrl(clean(request.getBusinessLogoUrl()));
        if (request.getSupportEmail() != null) settings.setSupportEmail(cleanLower(request.getSupportEmail()));
        if (request.getBusinessType() != null) settings.setBusinessType(normalizeChoice(request.getBusinessType(), BUSINESS_TYPES, "SINGLE_TURF"));
        if (request.getOperatingHours() != null) settings.setOperatingHours(clean(request.getOperatingHours()));
    }

    private void applyTurfConfiguration(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getDefaultSlotDurationMinutes() != null) settings.setDefaultSlotDurationMinutes(request.getDefaultSlotDurationMinutes());
        if (request.getOpeningTime() != null) settings.setOpeningTime(cleanOrDefault(request.getOpeningTime(), "06:00"));
        if (request.getClosingTime() != null) settings.setClosingTime(cleanOrDefault(request.getClosingTime(), "23:00"));
        if (request.getIndoorOutdoor() != null) settings.setIndoorOutdoor(normalizeChoice(request.getIndoorOutdoor(), ENVIRONMENTS, "OUTDOOR"));
        if (request.getAmenities() != null) settings.setAmenities(clean(request.getAmenities()));
    }

    private void applyBookingPolicy(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getAdvanceBookingDays() != null) settings.setAdvanceBookingDays(request.getAdvanceBookingDays());
        if (request.getMinimumBookingMinutes() != null) settings.setMinimumBookingMinutes(request.getMinimumBookingMinutes());
        if (request.getAllowInstantBooking() != null) settings.setAllowInstantBooking(request.getAllowInstantBooking());
        if (request.getApprovalRequired() != null) settings.setApprovalRequired(request.getApprovalRequired());
        if (request.getTrustedPlayersOnly() != null) settings.setTrustedPlayersOnly(request.getTrustedPlayersOnly());
        if (request.getCancellationPolicy() != null) settings.setCancellationPolicy(normalizeChoice(request.getCancellationPolicy(), CANCELLATION_POLICIES, "FLEXIBLE"));
    }

    private void applyPricing(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getDynamicPricingEnabled() != null) settings.setDynamicPricingEnabled(request.getDynamicPricingEnabled());
        if (request.getWeekendPricingEnabled() != null) settings.setWeekendPricingEnabled(request.getWeekendPricingEnabled());
        if (request.getPeakPricingEnabled() != null) settings.setPeakPricingEnabled(request.getPeakPricingEnabled());
        if (request.getPricingMode() != null) settings.setPricingMode(normalizeChoice(request.getPricingMode(), PRICING_MODES, "MANUAL"));
        if (request.getPeakMultiplier() != null) settings.setPeakMultiplier(request.getPeakMultiplier());
        if (request.getWeekendMultiplier() != null) settings.setWeekendMultiplier(request.getWeekendMultiplier());
    }

    private void applyStaff(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getStaffInviteEmail() != null) settings.setStaffInviteEmail(cleanLower(request.getStaffInviteEmail()));
        if (request.getStaffRole() != null) settings.setStaffRole(normalizeChoice(request.getStaffRole(), STAFF_ROLES, "MANAGER"));
        if (request.getStaffPermissions() != null) settings.setStaffPermissions(clean(request.getStaffPermissions()));
    }

    private void applyPayments(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getBankAccountName() != null) settings.setBankAccountName(clean(request.getBankAccountName()));
        if (request.getBankAccountNumber() != null) settings.setBankAccountNumber(clean(request.getBankAccountNumber()));
        if (request.getBankIfsc() != null) settings.setBankIfsc(cleanUpper(request.getBankIfsc()));
        if (request.getUpiId() != null) settings.setUpiId(cleanLower(request.getUpiId()));
        if (request.getSettlementSchedule() != null) settings.setSettlementSchedule(normalizeChoice(request.getSettlementSchedule(), List.of("DAILY", "WEEKLY", "MONTHLY"), "WEEKLY"));
        if (request.getPartialPaymentEnabled() != null) settings.setPartialPaymentEnabled(request.getPartialPaymentEnabled());
        if (request.getMinimumPaymentPercent() != null) settings.setMinimumPaymentPercent(request.getMinimumPaymentPercent());
        if (request.getRefundMode() != null) settings.setRefundMode(normalizeChoice(request.getRefundMode(), REFUND_MODES, "WALLET_CREDIT"));
    }

    private void applyNotifications(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getBookingAlerts() != null) settings.setBookingAlerts(request.getBookingAlerts());
        if (request.getPaymentAlerts() != null) settings.setPaymentAlerts(request.getPaymentAlerts());
        if (request.getReviewAlerts() != null) settings.setReviewAlerts(request.getReviewAlerts());
        if (request.getMarketingAlerts() != null) settings.setMarketingAlerts(request.getMarketingAlerts());
        if (request.getPromotionWhatsappEnabled() != null) settings.setPromotionWhatsappEnabled(request.getPromotionWhatsappEnabled());
        if (request.getReminderLeadMinutes() != null) settings.setReminderLeadMinutes(request.getReminderLeadMinutes());
    }

    private void applyWeather(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getRainRefundAllowed() != null) settings.setRainRefundAllowed(request.getRainRefundAllowed());
        if (request.getFreeWeatherReschedule() != null) settings.setFreeWeatherReschedule(request.getFreeWeatherReschedule());
        if (request.getWeatherRiskAlerts() != null) settings.setWeatherRiskAlerts(request.getWeatherRiskAlerts());
        if (request.getRainThresholdPercent() != null) settings.setRainThresholdPercent(request.getRainThresholdPercent());
    }

    private void applyMarketplace(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getResaleEnabled() != null) settings.setResaleEnabled(request.getResaleEnabled());
        if (request.getTransferEnabled() != null) settings.setTransferEnabled(request.getTransferEnabled());
        if (request.getMaxResaleMarkupPercent() != null) settings.setMaxResaleMarkupPercent(request.getMaxResaleMarkupPercent());
        if (request.getRestrictSuspiciousAccounts() != null) settings.setRestrictSuspiciousAccounts(request.getRestrictSuspiciousAccounts());
    }

    private void applyCommunity(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getAllowPublicTournaments() != null) settings.setAllowPublicTournaments(request.getAllowPublicTournaments());
        if (request.getCommunityVisibility() != null) settings.setCommunityVisibility(normalizeChoice(request.getCommunityVisibility(), VISIBILITY_OPTIONS, "PUBLIC"));
        if (request.getTeamRegistrationRule() != null) settings.setTeamRegistrationRule(normalizeChoice(request.getTeamRegistrationRule(), REGISTRATION_RULES, "APPROVAL_REQUIRED"));
    }

    private void applyBranding(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getBrandPrimaryColor() != null) settings.setBrandPrimaryColor(cleanOrDefault(request.getBrandPrimaryColor(), "#0EA5E9"));
        if (request.getBrandTheme() != null) settings.setBrandTheme(normalizeChoiceCaseSensitive(request.getBrandTheme(), THEMES, "system"));
        if (request.getCompactDashboard() != null) settings.setCompactDashboard(request.getCompactDashboard());
    }

    private void applySecurity(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getTwoFactorEnabled() != null) settings.setTwoFactorEnabled(request.getTwoFactorEnabled());
        if (request.getSessionTimeoutMinutes() != null) settings.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        if (request.getIpRestrictions() != null) settings.setIpRestrictions(clean(request.getIpRestrictions()));
    }

    private void applyAutomation(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getAutoPricingEnabled() != null) settings.setAutoPricingEnabled(request.getAutoPricingEnabled());
        if (request.getAutoRescheduleEnabled() != null) settings.setAutoRescheduleEnabled(request.getAutoRescheduleEnabled());
        if (request.getAiCoachEnabled() != null) settings.setAiCoachEnabled(request.getAiCoachEnabled());
        if (request.getSmartPromotionsEnabled() != null) settings.setSmartPromotionsEnabled(request.getSmartPromotionsEnabled());
    }

    private void applyReports(OwnerSettings settings, OwnerSettingsRequest request) {
        if (request.getRevenueExportEnabled() != null) settings.setRevenueExportEnabled(request.getRevenueExportEnabled());
        if (request.getGstReportsEnabled() != null) settings.setGstReportsEnabled(request.getGstReportsEnabled());
        if (request.getExportFormat() != null) settings.setExportFormat(normalizeChoice(request.getExportFormat(), EXPORT_FORMATS, "CSV"));
        if (request.getDataExportRequested() != null) {
            settings.setDataExportRequested(request.getDataExportRequested());
            if (request.getDataExportRequested()) {
                settings.setLastExportRequestedAt(LocalDateTime.now());
            }
        }
    }

    private User findOwner(String loginId) {
        User user = userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("Owner account was not found."));
        boolean allowed = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_OWNER || role.getName() == RoleName.ROLE_ADMIN);
        if (!allowed) {
            throw ApiException.forbidden("Only turf owners can manage owner settings.");
        }
        return user;
    }

    private OwnerSettings defaults(User owner) {
        return OwnerSettings.builder()
                .owner(owner)
                .businessName(owner.getName())
                .supportPhone(owner.getPhone())
                .supportEmail(owner.getEmail())
                .operatingHours("06:00 - 23:00")
                .staffPermissions("Bookings, slots, attendance, payouts")
                .build();
    }

    private OwnerSettings withDefaults(OwnerSettings settings, User owner) {
        if (settings.getOwner() == null) settings.setOwner(owner);
        if (settings.getBusinessName() == null) settings.setBusinessName(owner.getName());
        if (settings.getSupportPhone() == null) settings.setSupportPhone(owner.getPhone());
        if (settings.getSupportEmail() == null) settings.setSupportEmail(owner.getEmail());
        if (settings.getBusinessType() == null) settings.setBusinessType("SINGLE_TURF");
        if (settings.getOperatingHours() == null) settings.setOperatingHours("06:00 - 23:00");
        if (settings.getDefaultSlotDurationMinutes() == null) settings.setDefaultSlotDurationMinutes(60);
        if (settings.getOpeningTime() == null) settings.setOpeningTime("06:00");
        if (settings.getClosingTime() == null) settings.setClosingTime("23:00");
        if (settings.getIndoorOutdoor() == null) settings.setIndoorOutdoor("OUTDOOR");
        if (settings.getAdvanceBookingDays() == null) settings.setAdvanceBookingDays(14);
        if (settings.getMinimumBookingMinutes() == null) settings.setMinimumBookingMinutes(60);
        if (settings.getAllowInstantBooking() == null) settings.setAllowInstantBooking(Boolean.TRUE);
        if (settings.getApprovalRequired() == null) settings.setApprovalRequired(Boolean.FALSE);
        if (settings.getTrustedPlayersOnly() == null) settings.setTrustedPlayersOnly(Boolean.FALSE);
        if (settings.getCancellationPolicy() == null) settings.setCancellationPolicy("FLEXIBLE");
        if (settings.getDynamicPricingEnabled() == null) settings.setDynamicPricingEnabled(Boolean.FALSE);
        if (settings.getWeekendPricingEnabled() == null) settings.setWeekendPricingEnabled(Boolean.TRUE);
        if (settings.getPeakPricingEnabled() == null) settings.setPeakPricingEnabled(Boolean.TRUE);
        if (settings.getPricingMode() == null) settings.setPricingMode("MANUAL");
        if (settings.getPeakMultiplier() == null) settings.setPeakMultiplier(1.20d);
        if (settings.getWeekendMultiplier() == null) settings.setWeekendMultiplier(1.15d);
        if (settings.getStaffRole() == null) settings.setStaffRole("MANAGER");
        if (settings.getStaffPermissions() == null) settings.setStaffPermissions("Bookings, slots, attendance, payouts");
        if (settings.getSettlementSchedule() == null) settings.setSettlementSchedule("WEEKLY");
        if (settings.getBookingAlerts() == null) settings.setBookingAlerts(Boolean.TRUE);
        if (settings.getPaymentAlerts() == null) settings.setPaymentAlerts(Boolean.TRUE);
        if (settings.getReviewAlerts() == null) settings.setReviewAlerts(Boolean.TRUE);
        if (settings.getMarketingAlerts() == null) settings.setMarketingAlerts(Boolean.FALSE);
        if (settings.getPromotionWhatsappEnabled() == null) settings.setPromotionWhatsappEnabled(Boolean.FALSE);
        if (settings.getReminderLeadMinutes() == null) settings.setReminderLeadMinutes(120);
        if (settings.getPartialPaymentEnabled() == null) settings.setPartialPaymentEnabled(Boolean.TRUE);
        if (settings.getMinimumPaymentPercent() == null) settings.setMinimumPaymentPercent(50);
        if (settings.getRefundMode() == null) settings.setRefundMode("WALLET_CREDIT");
        if (settings.getRainRefundAllowed() == null) settings.setRainRefundAllowed(Boolean.TRUE);
        if (settings.getFreeWeatherReschedule() == null) settings.setFreeWeatherReschedule(Boolean.TRUE);
        if (settings.getWeatherRiskAlerts() == null) settings.setWeatherRiskAlerts(Boolean.TRUE);
        if (settings.getRainThresholdPercent() == null) settings.setRainThresholdPercent(70);
        if (settings.getResaleEnabled() == null) settings.setResaleEnabled(Boolean.TRUE);
        if (settings.getTransferEnabled() == null) settings.setTransferEnabled(Boolean.TRUE);
        if (settings.getMaxResaleMarkupPercent() == null) settings.setMaxResaleMarkupPercent(0);
        if (settings.getRestrictSuspiciousAccounts() == null) settings.setRestrictSuspiciousAccounts(Boolean.TRUE);
        if (settings.getAllowPublicTournaments() == null) settings.setAllowPublicTournaments(Boolean.TRUE);
        if (settings.getCommunityVisibility() == null) settings.setCommunityVisibility("PUBLIC");
        if (settings.getTeamRegistrationRule() == null) settings.setTeamRegistrationRule("APPROVAL_REQUIRED");
        if (settings.getBrandPrimaryColor() == null) settings.setBrandPrimaryColor("#0EA5E9");
        if (settings.getBrandTheme() == null) settings.setBrandTheme("system");
        if (settings.getCompactDashboard() == null) settings.setCompactDashboard(Boolean.FALSE);
        if (settings.getTwoFactorEnabled() == null) settings.setTwoFactorEnabled(Boolean.FALSE);
        if (settings.getSessionTimeoutMinutes() == null) settings.setSessionTimeoutMinutes(60);
        if (settings.getAutoPricingEnabled() == null) settings.setAutoPricingEnabled(Boolean.FALSE);
        if (settings.getAutoRescheduleEnabled() == null) settings.setAutoRescheduleEnabled(Boolean.TRUE);
        if (settings.getAiCoachEnabled() == null) settings.setAiCoachEnabled(Boolean.TRUE);
        if (settings.getSmartPromotionsEnabled() == null) settings.setSmartPromotionsEnabled(Boolean.TRUE);
        if (settings.getRevenueExportEnabled() == null) settings.setRevenueExportEnabled(Boolean.TRUE);
        if (settings.getGstReportsEnabled() == null) settings.setGstReportsEnabled(Boolean.TRUE);
        if (settings.getExportFormat() == null) settings.setExportFormat("CSV");
        if (settings.getDataExportRequested() == null) settings.setDataExportRequested(Boolean.FALSE);
        return settings;
    }

    private void validate(OwnerSettings settings) {
        if (settings.getSupportPhone() != null && !settings.getSupportPhone().matches("^[+]?[0-9\\s-]{7,18}$")) {
            throw ApiException.badRequest("Support phone must be a valid phone number.");
        }
        if (settings.getSupportEmail() != null && !settings.getSupportEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.badRequest("Support email must be a valid email address.");
        }
        if (settings.getGstNumber() != null && !settings.getGstNumber().matches("^[0-9A-Z]{15}$")) {
            throw ApiException.badRequest("GST number must be 15 uppercase letters or digits.");
        }
        if (settings.getBankIfsc() != null && !settings.getBankIfsc().matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            throw ApiException.badRequest("IFSC code must be valid, for example HDFC0001234.");
        }
        if (settings.getUpiId() != null && !settings.getUpiId().matches("^[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}$")) {
            throw ApiException.badRequest("UPI ID must be valid, for example owner@upi.");
        }
        requireRange(settings.getDefaultSlotDurationMinutes(), 15, 240, "Default slot duration must be between 15 and 240 minutes.");
        requireRange(settings.getAdvanceBookingDays(), 1, 365, "Advance booking limit must be between 1 and 365 days.");
        requireRange(settings.getMinimumBookingMinutes(), 15, 240, "Minimum booking duration must be between 15 and 240 minutes.");
        requireRange(settings.getReminderLeadMinutes(), 15, 1440, "Reminder timing must be between 15 minutes and 24 hours.");
        requireRange(settings.getMinimumPaymentPercent(), 0, 100, "Minimum payment percent must be between 0 and 100.");
        requireRange(settings.getRainThresholdPercent(), 0, 100, "Rain threshold must be between 0 and 100 percent.");
        requireRange(settings.getMaxResaleMarkupPercent(), 0, 100, "Max resale markup must be between 0 and 100 percent.");
        requireRange(settings.getSessionTimeoutMinutes(), 15, 1440, "Session timeout must be between 15 minutes and 24 hours.");
        requireMultiplier(settings.getPeakMultiplier(), "Peak multiplier must be between 1.0 and 3.0.");
        requireMultiplier(settings.getWeekendMultiplier(), "Weekend multiplier must be between 1.0 and 3.0.");
        if (!settings.getOpeningTime().matches("^([01]\\d|2[0-3]):[0-5]\\d$") || !settings.getClosingTime().matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
            throw ApiException.badRequest("Opening and closing time must use HH:mm format.");
        }
        if (settings.getBrandPrimaryColor() != null && !settings.getBrandPrimaryColor().matches("^#[0-9A-Fa-f]{6}$")) {
            throw ApiException.badRequest("Brand color must be a valid hex color, for example #0EA5E9.");
        }
    }

    private OwnerSettingsResponse map(User owner, OwnerSettings settings) {
        return OwnerSettingsResponse.builder()
                .ownerId(owner.getId())
                .ownerName(owner.getName())
                .ownerEmail(owner.getEmail())
                .ownerPhone(owner.getPhone())
                .accountStatus(owner.getStatus())
                .preferredLanguage(owner.getPreferredLanguage())
                .preferredTheme(owner.getPreferredTheme())
                .businessName(settings.getBusinessName())
                .supportPhone(settings.getSupportPhone())
                .businessAddress(settings.getBusinessAddress())
                .gstNumber(settings.getGstNumber())
                .businessLogoUrl(settings.getBusinessLogoUrl())
                .supportEmail(settings.getSupportEmail())
                .businessType(settings.getBusinessType())
                .operatingHours(settings.getOperatingHours())
                .defaultSlotDurationMinutes(settings.getDefaultSlotDurationMinutes())
                .openingTime(settings.getOpeningTime())
                .closingTime(settings.getClosingTime())
                .indoorOutdoor(settings.getIndoorOutdoor())
                .amenities(settings.getAmenities())
                .advanceBookingDays(settings.getAdvanceBookingDays())
                .minimumBookingMinutes(settings.getMinimumBookingMinutes())
                .allowInstantBooking(settings.getAllowInstantBooking())
                .approvalRequired(settings.getApprovalRequired())
                .trustedPlayersOnly(settings.getTrustedPlayersOnly())
                .cancellationPolicy(settings.getCancellationPolicy())
                .dynamicPricingEnabled(settings.getDynamicPricingEnabled())
                .weekendPricingEnabled(settings.getWeekendPricingEnabled())
                .peakPricingEnabled(settings.getPeakPricingEnabled())
                .pricingMode(settings.getPricingMode())
                .peakMultiplier(settings.getPeakMultiplier())
                .weekendMultiplier(settings.getWeekendMultiplier())
                .staffInviteEmail(settings.getStaffInviteEmail())
                .staffRole(settings.getStaffRole())
                .staffPermissions(settings.getStaffPermissions())
                .bankAccountName(settings.getBankAccountName())
                .bankAccountNumber(settings.getBankAccountNumber())
                .bankIfsc(settings.getBankIfsc())
                .upiId(settings.getUpiId())
                .settlementSchedule(settings.getSettlementSchedule())
                .bookingAlerts(settings.getBookingAlerts())
                .paymentAlerts(settings.getPaymentAlerts())
                .reviewAlerts(settings.getReviewAlerts())
                .marketingAlerts(settings.getMarketingAlerts())
                .promotionWhatsappEnabled(settings.getPromotionWhatsappEnabled())
                .reminderLeadMinutes(settings.getReminderLeadMinutes())
                .partialPaymentEnabled(settings.getPartialPaymentEnabled())
                .minimumPaymentPercent(settings.getMinimumPaymentPercent())
                .refundMode(settings.getRefundMode())
                .rainRefundAllowed(settings.getRainRefundAllowed())
                .freeWeatherReschedule(settings.getFreeWeatherReschedule())
                .weatherRiskAlerts(settings.getWeatherRiskAlerts())
                .rainThresholdPercent(settings.getRainThresholdPercent())
                .resaleEnabled(settings.getResaleEnabled())
                .transferEnabled(settings.getTransferEnabled())
                .maxResaleMarkupPercent(settings.getMaxResaleMarkupPercent())
                .restrictSuspiciousAccounts(settings.getRestrictSuspiciousAccounts())
                .allowPublicTournaments(settings.getAllowPublicTournaments())
                .communityVisibility(settings.getCommunityVisibility())
                .teamRegistrationRule(settings.getTeamRegistrationRule())
                .brandPrimaryColor(settings.getBrandPrimaryColor())
                .brandTheme(settings.getBrandTheme())
                .compactDashboard(settings.getCompactDashboard())
                .twoFactorEnabled(settings.getTwoFactorEnabled())
                .sessionTimeoutMinutes(settings.getSessionTimeoutMinutes())
                .ipRestrictions(settings.getIpRestrictions())
                .autoPricingEnabled(settings.getAutoPricingEnabled())
                .autoRescheduleEnabled(settings.getAutoRescheduleEnabled())
                .aiCoachEnabled(settings.getAiCoachEnabled())
                .smartPromotionsEnabled(settings.getSmartPromotionsEnabled())
                .revenueExportEnabled(settings.getRevenueExportEnabled())
                .gstReportsEnabled(settings.getGstReportsEnabled())
                .exportFormat(settings.getExportFormat())
                .dataExportRequested(settings.getDataExportRequested())
                .lastExportRequestedAt(settings.getLastExportRequestedAt() == null ? null : settings.getLastExportRequestedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .readinessScore(readinessScore(settings))
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    private Integer readinessScore(OwnerSettings settings) {
        List<String> values = List.of(
                safe(settings.getBusinessName()),
                safe(settings.getSupportPhone()),
                safe(settings.getSupportEmail()),
                safe(settings.getBusinessAddress()),
                safe(settings.getOpeningTime()),
                safe(settings.getClosingTime()),
                safe(settings.getBankAccountName()),
                safe(settings.getUpiId()),
                safe(settings.getCancellationPolicy()),
                safe(settings.getPricingMode())
        );
        long filled = values.stream().filter(value -> value != null && !value.toString().isBlank()).count();
        return (int) Math.round((filled * 100.0d) / values.size());
    }

    private void requireRange(Integer value, int min, int max, String message) {
        if (value == null || value < min || value > max) {
            throw ApiException.badRequest(message);
        }
    }

    private void requireMultiplier(Double value, String message) {
        if (value == null || value < 1.0d || value > 3.0d) {
            throw ApiException.badRequest(message);
        }
    }

    private String normalizeChoice(String value, List<String> allowed, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("Unsupported value: " + value + ".");
        }
        return normalized;
    }

    private String normalizeChoiceCaseSensitive(String value, List<String> allowed, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("Unsupported value: " + value + ".");
        }
        return normalized;
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String cleanUpper(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toUpperCase(Locale.ROOT);
    }

    private String cleanLower(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}
