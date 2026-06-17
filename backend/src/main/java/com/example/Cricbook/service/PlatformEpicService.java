package com.example.Cricbook.service;

import com.example.Cricbook.dto.PlatformEpicChecklistResponse;
import com.example.Cricbook.dto.PlatformEpicResponse;
import com.example.Cricbook.dto.PlatformEpicSurfaceResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformEpicService {

    public List<PlatformEpicResponse> getEpics() {
        return List.of(platformArchitectureEpic(), reliabilityTemplate(), autoFillTemplate());
    }

    public PlatformEpicResponse getCurrentEpic() {
        return platformArchitectureEpic();
    }

    private PlatformEpicResponse platformArchitectureEpic() {
        return PlatformEpicResponse.builder()
                .epicId("EPIC-23")
                .name("Platform-Wide Epic Governance")
                .objective("Every future feature ships as one backend contract with web, owner, admin, mobile, notification, analytics, theme, and offline considerations.")
                .releaseStage("ACTIVE")
                .defaultTheme("player=emerald, owner=blue, admin=indigo")
                .sharedDomains(List.of("Auth", "Booking", "Payments", "Notifications", "Analytics", "AI", "Trust", "Theme", "Localization"))
                .surfaces(List.of(
                        surface("PLAYER_WEB", "READY", "Frontend", List.of("Player navigation", "Personalized discovery", "Booking actions", "Wallet and rewards")),
                        surface("OWNER_WEB", "READY", "Frontend", List.of("Owner dashboard", "Slot manager", "Bookings", "Reviews", "Earnings")),
                        surface("ADMIN_WEB", "READY", "Frontend", List.of("Platform governance", "Owner verification", "Campaigns", "Incidents", "Analytics")),
                        surface("MOBILE", "READY", "Mobile", List.of("Player shell", "Owner shell", "Admin shell", "Theme and language controls", "Push-ready notification screen")),
                        surface("BACKEND", "READY", "Spring Boot", List.of("Shared APIs", "Role-aware security", "Notification queue", "Analytics services"))
                ))
                .readiness(List.of(
                        check("backend_contract", "Shared Spring Boot API contract exists before UI work starts", "PASS"),
                        check("web_player", "Player web flow is represented", "PASS"),
                        check("web_owner", "Owner portal flow is represented", "PASS"),
                        check("web_admin", "Admin governance flow is represented", "PASS"),
                        check("mobile", "Mobile player, owner, and admin entry points are represented", "PASS"),
                        check("notifications", "Push or in-app notification event names are defined", "PASS"),
                        check("analytics", "Analytics event names are defined", "PASS"),
                        check("theme", "Player and owner theme ownership is defined", "PASS"),
                        check("offline_realtime", "Offline/realtime behavior is explicitly marked for each epic", "PASS")
                ))
                .notificationEvents(List.of(
                        "EPIC_GOVERNANCE_UPDATED",
                        "FEATURE_SURFACE_READY",
                        "MOBILE_PUSH_REQUIRED",
                        "OWNER_WORKFLOW_READY"
                ))
                .analyticsEvents(List.of(
                        "epic.surface.viewed",
                        "epic.readiness.checked",
                        "epic.mobile.opened",
                        "epic.admin.reviewed"
                ))
                .build();
    }

    private PlatformEpicResponse reliabilityTemplate() {
        return PlatformEpicResponse.builder()
                .epicId("EPIC-24")
                .name("Reliability System Template")
                .objective("Trust badges, high-risk booking warnings, attendance confirmation, and owner risk analytics.")
                .releaseStage("PLANNED")
                .defaultTheme("trust=emerald, risk=amber/red, owner=blue")
                .sharedDomains(List.of("Trust", "Booking", "Notifications", "Analytics"))
                .surfaces(List.of(
                        surface("PLAYER_WEB", "PLANNED", "Frontend", List.of("Profile trust badge", "Report issue", "Attendance confirmation")),
                        surface("OWNER_WEB", "PLANNED", "Frontend", List.of("High-risk booking warning", "Trust filters", "Reliability analytics")),
                        surface("ADMIN_WEB", "PLANNED", "Frontend", List.of("Trust audit queue", "Abuse patterns", "Manual adjustments")),
                        surface("MOBILE", "PLANNED", "Mobile", List.of("Trust badge", "Quick reports", "Attendance push action")),
                        surface("BACKEND", "PLANNED", "Spring Boot", List.of("Reliability score API", "Report API", "Audit events"))
                ))
                .readiness(defaultPlannedReadiness())
                .notificationEvents(List.of("TRUST_BADGE_EARNED", "ATTENDANCE_CONFIRMATION_REQUIRED", "HIGH_RISK_BOOKING_WARNING"))
                .analyticsEvents(List.of("trust.badge.earned", "booking.risk.flagged", "attendance.confirmed"))
                .build();
    }

    private PlatformEpicResponse autoFillTemplate() {
        return PlatformEpicResponse.builder()
                .epicId("EPIC-25")
                .name("Auto Player Fill Template")
                .objective("Fill open match slots using nearby player availability, recommendations, and push notifications.")
                .releaseStage("PLANNED")
                .defaultTheme("player=emerald, owner=blue")
                .sharedDomains(List.of("Matches", "Teams", "Notifications", "Geo", "AI", "Analytics"))
                .surfaces(List.of(
                        surface("PLAYER_WEB", "PLANNED", "Frontend", List.of("Open spots", "Join recommendations", "Availability profile")),
                        surface("OWNER_WEB", "PLANNED", "Frontend", List.of("Fill-rate analytics", "Match demand view")),
                        surface("ADMIN_WEB", "PLANNED", "Frontend", List.of("Auto-fill controls", "Notification safety")),
                        surface("MOBILE", "PLANNED", "Mobile", List.of("Nearby players push", "One-tap join", "Availability toggle")),
                        surface("BACKEND", "PLANNED", "Spring Boot", List.of("Matching engine", "Availability API", "Push dispatch"))
                ))
                .readiness(defaultPlannedReadiness())
                .notificationEvents(List.of("NEED_PLAYERS_NEARBY", "MATCH_SLOT_FILLED", "PLAYER_INVITE_EXPIRED"))
                .analyticsEvents(List.of("autofill.invite.sent", "autofill.join.accepted", "autofill.slot.filled"))
                .build();
    }

    private List<PlatformEpicChecklistResponse> defaultPlannedReadiness() {
        return List.of(
                check("backend_contract", "Shared API contract defined", "PLANNED"),
                check("web_player", "Player web flow defined", "PLANNED"),
                check("web_owner", "Owner web flow defined", "PLANNED"),
                check("web_admin", "Admin flow defined", "PLANNED"),
                check("mobile", "Mobile flow and navigation defined", "PLANNED"),
                check("notifications", "Notification events defined", "PLANNED"),
                check("analytics", "Analytics events defined", "PLANNED"),
                check("theme", "Theme behavior defined", "PLANNED"),
                check("offline_realtime", "Offline/realtime behavior defined", "PLANNED")
        );
    }

    private PlatformEpicSurfaceResponse surface(String surface, String status, String owner, List<String> capabilities) {
        return PlatformEpicSurfaceResponse.builder()
                .surface(surface)
                .status(status)
                .owner(owner)
                .capabilities(capabilities)
                .build();
    }

    private PlatformEpicChecklistResponse check(String key, String label, String status) {
        return PlatformEpicChecklistResponse.builder()
                .key(key)
                .label(label)
                .status(status)
                .build();
    }
}
