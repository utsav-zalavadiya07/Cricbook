package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SYSTEM_CONFIG_VIEW')")
public class AdminSystemController {

    private final PlatformSettingRepository platformSettingRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final MaintenanceConfigRepository maintenanceConfigRepository;
    private final ReleaseVersionRepository releaseVersionRepository;
    private final ConfigurationAuditRepository configurationAuditRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        seedDefaults();
        List<FeatureFlag> flags = featureFlagRepository.findAll();
        MaintenanceConfig maintenance = maintenanceConfig();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeFeatures", flags.stream().filter(flag -> Boolean.TRUE.equals(flag.getEnabled())).count());
        response.put("totalFeatures", flags.size());
        response.put("maintenanceStatus", maintenance.getEnabled());
        response.put("currentVersion", releaseVersionRepository.findAllByOrderByCreatedAtDesc().stream().findFirst().map(ReleaseVersion::getVersion).orElse("1.0.0"));
        response.put("recentConfigChanges", configurationAuditRepository.findTop100ByOrderByChangedAtDesc().stream().limit(5).toList());
        response.put("pendingReleases", releaseVersionRepository.findAll().stream().filter(release -> "DRAFT".equalsIgnoreCase(release.getStatus()) || "SCHEDULED".equalsIgnoreCase(release.getStatus())).count());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<List<PlatformSetting>> settings() {
        seedDefaults();
        return ResponseEntity.ok(platformSettingRepository.findAll());
    }

    @PostMapping("/settings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SYSTEM_CONFIG_EDIT')")
    public ResponseEntity<PlatformSetting> saveSetting(@RequestBody SettingRequest request) {
        PlatformSetting setting = platformSettingRepository.findByKey(required(request.getKey(), "Setting key is required"))
                .orElseGet(() -> PlatformSetting.builder().key(request.getKey()).build());
        String oldValue = setting.getValue();
        setting.setValue(required(request.getValue(), "Setting value is required"));
        PlatformSetting saved = platformSettingRepository.save(setting);
        audit(saved.getKey(), oldValue, saved.getValue());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/settings/{domain}")
    public ResponseEntity<List<PlatformSetting>> settingsByDomain(@PathVariable String domain) {
        String prefix = domain.toUpperCase(Locale.ROOT) + "_";
        return ResponseEntity.ok(platformSettingRepository.findAll().stream()
                .filter(setting -> setting.getKey() != null && setting.getKey().startsWith(prefix))
                .toList());
    }

    @GetMapping("/feature-flags")
    public ResponseEntity<List<FeatureFlag>> featureFlags() {
        seedDefaults();
        return ResponseEntity.ok(featureFlagRepository.findAllByOrderByUpdatedAtDesc());
    }

    @PostMapping("/feature-flags")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('FEATURE_FLAG_EDIT')")
    public ResponseEntity<FeatureFlag> saveFeatureFlag(@RequestBody FeatureFlag request) {
        FeatureFlag flag = featureFlagRepository.findByFeatureKey(required(request.getFeatureKey(), "Feature key is required"))
                .orElseGet(FeatureFlag::new);
        String oldValue = flag.getEnabled() == null ? null : flag.getEnabled().toString();
        flag.setFeatureKey(request.getFeatureKey());
        flag.setEnabled(request.getEnabled() != null && request.getEnabled());
        flag.setRolloutPercentage(request.getRolloutPercentage() == null ? 0 : Math.max(0, Math.min(100, request.getRolloutPercentage())));
        flag.setTargetAudience(defaultString(request.getTargetAudience(), "ALL"));
        flag.setDescription(request.getDescription());
        FeatureFlag saved = featureFlagRepository.save(flag);
        audit("FEATURE_FLAG_" + saved.getFeatureKey(), oldValue, saved.getEnabled() + ":" + saved.getRolloutPercentage() + ":" + saved.getTargetAudience());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/maintenance")
    public ResponseEntity<MaintenanceConfig> maintenance() {
        return ResponseEntity.ok(maintenanceConfig());
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('MAINTENANCE_CONTROL')")
    public ResponseEntity<MaintenanceConfig> saveMaintenance(@RequestBody MaintenanceConfig request) {
        MaintenanceConfig config = maintenanceConfig();
        String oldValue = config.getEnabled() + ":" + config.getMessage();
        config.setEnabled(request.getEnabled() != null && request.getEnabled());
        config.setMessage(defaultString(request.getMessage(), "Platform is under maintenance."));
        config.setStartTime(request.getStartTime());
        config.setEndTime(request.getEndTime());
        config.setAllowAdminAccess(request.getAllowAdminAccess() == null || request.getAllowAdminAccess());
        MaintenanceConfig saved = maintenanceConfigRepository.save(config);
        saveSettingValue("MAINTENANCE_MODE", saved.getEnabled().toString());
        audit("MAINTENANCE_MODE", oldValue, saved.getEnabled() + ":" + saved.getMessage());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/releases")
    public ResponseEntity<List<ReleaseVersion>> releases() {
        return ResponseEntity.ok(releaseVersionRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/releases")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('RELEASE_MANAGE')")
    public ResponseEntity<ReleaseVersion> saveRelease(@RequestBody ReleaseVersion request) {
        ReleaseVersion release = releaseVersionRepository.findByVersion(required(request.getVersion(), "Version is required"))
                .orElseGet(ReleaseVersion::new);
        String oldValue = release.getStatus();
        release.setVersion(request.getVersion());
        release.setReleaseDate(request.getReleaseDate() == null ? LocalDate.now() : request.getReleaseDate());
        release.setNotes(defaultString(request.getNotes(), "Release notes pending."));
        release.setStatus(defaultString(request.getStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        release.setRolloutPercentage(request.getRolloutPercentage() == null ? 0 : Math.max(0, Math.min(100, request.getRolloutPercentage())));
        ReleaseVersion saved = releaseVersionRepository.save(release);
        audit("RELEASE_" + saved.getVersion(), oldValue, saved.getStatus() + ":" + saved.getRolloutPercentage());
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/audit")
    public ResponseEntity<List<ConfigurationAudit>> auditLog() {
        return ResponseEntity.ok(configurationAuditRepository.findTop100ByOrderByChangedAtDesc());
    }

    @PostMapping("/seed-defaults")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('SYSTEM_CONFIG_EDIT')")
    public ResponseEntity<Map<String, Object>> seed() {
        seedDefaults();
        return ResponseEntity.ok(Map.of("settings", platformSettingRepository.count(), "featureFlags", featureFlagRepository.count()));
    }

    private MaintenanceConfig maintenanceConfig() {
        return maintenanceConfigRepository.findAll().stream().findFirst()
                .orElseGet(() -> maintenanceConfigRepository.save(MaintenanceConfig.builder().enabled(false).message("Platform is operating normally.").allowAdminAccess(true).build()));
    }

    private void seedDefaults() {
        saveSettingIfMissing("GLOBAL_PLATFORM_NAME", "Cricbook");
        saveSettingIfMissing("GLOBAL_SUPPORT_EMAIL", "support@cricbook.com");
        saveSettingIfMissing("GLOBAL_SUPPORT_PHONE", "+91-00000-00000");
        saveSettingIfMissing("GLOBAL_DEFAULT_CURRENCY", "INR");
        saveSettingIfMissing("GLOBAL_DEFAULT_TIMEZONE", "Asia/Kolkata");
        saveSettingIfMissing("GLOBAL_COUNTRY", "India");
        saveSettingIfMissing("BOOKING_MAX_DURATION_HOURS", "4");
        saveSettingIfMissing("BOOKING_MIN_DURATION_HOURS", "1");
        saveSettingIfMissing("BOOKING_ADVANCE_DAYS", "30");
        saveSettingIfMissing("BOOKING_CANCELLATION_WINDOW_HOURS", "6");
        saveSettingIfMissing("PAYMENT_PLATFORM_COMMISSION", platformSettingRepository.findByKey("COMMISSION_RATE").map(PlatformSetting::getValue).orElse("10.0"));
        saveSettingIfMissing("PAYMENT_GST_PERCENT", "18");
        saveSettingIfMissing("PAYMENT_REFUND_LIMIT_DAYS", "7");
        saveSettingIfMissing("TRUST_DEFAULT_SCORE", "100");
        saveSettingIfMissing("TRUST_NO_SHOW_PENALTY", "10");
        saveSettingIfMissing("TRUST_WARNING_THRESHOLD", "60");
        saveSettingIfMissing("NOTIFICATION_BOOKING_REMINDER_HOURS", "2");
        saveSettingIfMissing("NOTIFICATION_PROMOTION_FREQUENCY_DAYS", "7");
        saveSettingIfMissing("TOURNAMENT_MAX_TEAMS", "32");
        saveSettingIfMissing("TOURNAMENT_REGISTRATION_LIMIT_DAYS", "14");
        saveSettingIfMissing("COMMUNITY_POST_APPROVAL_REQUIRED", "false");
        saveSettingIfMissing("COMMUNITY_MEDIA_UPLOAD_LIMIT_MB", "10");
        saveSettingIfMissing("ENVIRONMENT_NAME", "production");
        saveSettingIfMissing("ENVIRONMENT_READ_ONLY_MODE", "false");
        saveSettingIfMissing("ENVIRONMENT_DEBUG_MODE", "false");
        seedFlag("ENABLE_TOURNAMENTS", true, 100, "ALL", "Tournament module rollout");
        seedFlag("ENABLE_COMMUNITY", true, 100, "ALL", "Community and social surfaces");
        seedFlag("ENABLE_WALLET", true, 100, "ALL", "Wallet and credits");
        seedFlag("ENABLE_REFERRAL_PROGRAM", true, 100, "ALL", "Referral growth loop");
        seedFlag("ENABLE_DARK_MODE", true, 100, "ALL", "Dark mode theme");
        if (releaseVersionRepository.count() == 0) {
            releaseVersionRepository.save(ReleaseVersion.builder().version("1.0.0").releaseDate(LocalDate.now()).notes("Initial platform control center baseline.").status("ACTIVE").rolloutPercentage(100).build());
        }
        maintenanceConfig();
    }

    private void seedFlag(String key, boolean enabled, int rollout, String audience, String description) {
        if (featureFlagRepository.findByFeatureKey(key).isEmpty()) {
            featureFlagRepository.save(FeatureFlag.builder().featureKey(key).enabled(enabled).rolloutPercentage(rollout).targetAudience(audience).description(description).build());
        }
    }

    private void saveSettingIfMissing(String key, String value) {
        if (platformSettingRepository.findByKey(key).isEmpty()) {
            platformSettingRepository.save(PlatformSetting.builder().key(key).value(value).build());
        }
    }

    private void saveSettingValue(String key, String value) {
        PlatformSetting setting = platformSettingRepository.findByKey(key).orElseGet(() -> PlatformSetting.builder().key(key).build());
        setting.setValue(value);
        platformSettingRepository.save(setting);
    }

    private void audit(String key, String oldValue, String newValue) {
        configurationAuditRepository.save(ConfigurationAudit.builder()
                .configKey(key)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(actor())
                .build());
    }

    private String actor() {
        return SecurityContextHolder.getContext().getAuthentication() == null ? "system-admin" : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @Data
    public static class SettingRequest {
        private String key;
        private String value;
    }
}
