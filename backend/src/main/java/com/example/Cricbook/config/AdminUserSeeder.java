package com.example.Cricbook.config;

import com.example.Cricbook.entity.AdminUser;
import com.example.Cricbook.entity.AdminPermission;
import com.example.Cricbook.entity.AdminRole;
import com.example.Cricbook.entity.PermissionGroup;
import com.example.Cricbook.entity.PlatformSetting;
import com.example.Cricbook.repository.AdminPermissionRepository;
import com.example.Cricbook.repository.AdminRoleRepository;
import com.example.Cricbook.repository.AdminUserRepository;
import com.example.Cricbook.repository.PermissionGroupRepository;
import com.example.Cricbook.repository.PlatformSettingRepository;
import com.example.Cricbook.security.PortalPagePermissionCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedAdminRbac();

        if (!adminUserRepository.existsByEmail("admin@cricbook.com")) {
            AdminUser admin = AdminUser.builder()
                    .email("admin@cricbook.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role("SUPER_ADMIN")
                    .status("ACTIVE")
                    .build();
            adminUserRepository.save(admin);
            System.out.println("--- Seeded Super Admin User: admin@cricbook.com / admin123 ---");
        }

        if (platformSettingRepository.findByKey("COMMISSION_RATE").isEmpty()) {
            PlatformSetting setting = PlatformSetting.builder()
                    .key("COMMISSION_RATE")
                    .value("10.0")
                    .build();
            platformSettingRepository.save(setting);
            System.out.println("--- Seeded Commission Rate: 10.0% ---");
        }
    }

    private void seedAdminRbac() {
        Map<String, String> permissions = new LinkedHashMap<>(Map.ofEntries(
                Map.entry("USER_VIEW", "View players and owner accounts"),
                Map.entry("USER_EDIT", "Edit or moderate user accounts"),
                Map.entry("USER_DELETE", "Delete duplicate or invalid accounts"),
                Map.entry("USER_SUSPEND", "Suspend risky or policy-breaking users"),
                Map.entry("OWNER_APPROVE", "Approve and reject owner verification"),
                Map.entry("OWNER_REJECT", "Reject owner verification requests"),
                Map.entry("OWNER_STAFF_MANAGE", "Assign owner staff access roles"),
                Map.entry("TURF_MODERATE", "Approve, reject, and deactivate turfs"),
                Map.entry("BOOKING_VIEW", "View platform bookings"),
                Map.entry("BOOKING_EDIT", "Override booking states and disputes"),
                Map.entry("BOOKING_CANCEL", "Cancel bookings during disputes"),
                Map.entry("DISPUTE_RESOLVE", "Resolve booking disputes and evidence reviews"),
                Map.entry("REFUND_OVERRIDE", "Override booking refund decisions during operations"),
                Map.entry("ESCALATION_MANAGE", "Manage critical booking escalations"),
                Map.entry("PAYMENT_VIEW", "View transactions, wallets, refunds, and payouts"),
                Map.entry("PAYMENT_EDIT", "Investigate and update financial records"),
                Map.entry("PAYMENT_REFUND", "Issue manual refunds"),
                Map.entry("PAYMENT_PAYOUT", "Manage owner payouts"),
                Map.entry("PAYMENT_SETTLEMENT", "Reconcile settlements and commissions"),
                Map.entry("REFUND_APPROVE", "Approve, reject, and force refund requests"),
                Map.entry("SETTLEMENT_PROCESS", "Generate and process owner settlements"),
                Map.entry("WALLET_ADJUST", "Credit, debit, and correct wallet balances"),
                Map.entry("COMMISSION_EDIT", "Create and update commission rules"),
                Map.entry("TRUST_MODERATE", "Moderate trust reports and override trust state"),
                Map.entry("COMMUNITY_MODERATE", "Moderate posts, teams, comments, and media"),
                Map.entry("REPORT_RESOLVE", "Resolve user safety reports and investigations"),
                Map.entry("USER_WARN", "Issue user safety warnings"),
                Map.entry("USER_BAN", "Ban and unban unsafe accounts or identifiers"),
                Map.entry("TRUST_EDIT", "Adjust player trust scores after moderation review"),
                Map.entry("MEDIA_MODERATE", "Approve, reject, blur, or remove uploaded media"),
                Map.entry("APPEAL_REVIEW", "Approve or reject moderation appeals"),
                Map.entry("TEAM_MODERATE", "Hide or suspend teams after policy review"),
                Map.entry("REVIEW_MODERATE", "Moderate turf, owner, and player reviews"),
                Map.entry("TOURNAMENT_MANAGE", "Create, approve, and close tournaments"),
                Map.entry("SUPPORT_MANAGE", "Assign, reply, escalate, and resolve support tickets"),
                Map.entry("TICKET_VIEW", "View support tickets and customer issue queues"),
                Map.entry("TICKET_ASSIGN", "Assign and escalate support tickets"),
                Map.entry("TICKET_RESOLVE", "Reply to and resolve support tickets"),
                Map.entry("CHAT_ACCESS", "Access live support chat and unified inbox"),
                Map.entry("FAQ_EDIT", "Create and publish FAQ entries"),
                Map.entry("KB_EDIT", "Create and publish knowledge base articles"),
                Map.entry("CSAT_VIEW", "View customer satisfaction feedback"),
                Map.entry("CMS_MANAGE", "Manage banners, FAQ, terms, announcements, and promotions"),
                Map.entry("CMS_EDIT", "Create and publish CMS pages, banners, announcements, and landing pages"),
                Map.entry("COUPON_CREATE", "Create and manage promotional coupons"),
                Map.entry("CAMPAIGN_CREATE", "Create marketing campaigns and promotional campaigns"),
                Map.entry("EMAIL_SEND", "Create and send email campaigns"),
                Map.entry("PUSH_SEND", "Create and send push campaigns"),
                Map.entry("SEO_EDIT", "Edit SEO metadata and Open Graph settings"),
                Map.entry("REPORT_VIEW", "View platform reports and analytics"),
                Map.entry("ANALYTICS_VIEW", "View executive platform analytics and KPI dashboards"),
                Map.entry("REVENUE_ANALYTICS_VIEW", "View revenue, commission, and refund analytics"),
                Map.entry("FORECAST_VIEW", "View forecasting models and projections"),
                Map.entry("AI_INSIGHTS_VIEW", "View AI business recommendations and alerts"),
                Map.entry("EXPORT_REPORTS", "Export analytics reports and executive summaries"),
                Map.entry("ROLE_MANAGE", "Create roles and assign permissions"),
                Map.entry("PERMISSION_VIEW", "View permissions and permission groups"),
                Map.entry("PERMISSION_EDIT", "Create and update permission groups"),
                Map.entry("POLICY_MANAGE", "Create access policies"),
                Map.entry("RESOURCE_PERMISSION_MANAGE", "Grant resource-level permissions"),
                Map.entry("TEMPORARY_ACCESS_GRANT", "Grant short-term elevated permissions"),
                Map.entry("AUDIT_VIEW", "View audit logs"),
                Map.entry("SECURITY_AUDIT_VIEW", "View security and permission audit logs"),
                Map.entry("SYSTEM_SETTINGS_EDIT", "Edit global platform settings"),
                Map.entry("SYSTEM_CONFIG_VIEW", "View system configuration, feature flags, maintenance, and releases"),
                Map.entry("SYSTEM_CONFIG_EDIT", "Edit system configuration values and business rules"),
                Map.entry("FEATURE_FLAG_EDIT", "Enable, disable, and target feature flags"),
                Map.entry("MAINTENANCE_CONTROL", "Control platform maintenance and read-only modes"),
                Map.entry("RELEASE_MANAGE", "Create releases, manage rollout, and rollback versions"),
                Map.entry("SECURITY_VIEW", "View security events and suspicious activity"),
                Map.entry("MONITORING_VIEW", "View API, DB, payments, notifications, and storage health"),
                Map.entry("API_MANAGE", "Manage API access and integrations"),
                Map.entry("FEATURE_FLAG_MANAGE", "Manage feature flags and platform rollout controls"),
                Map.entry("TEAM_DELETE", "Delete teams after policy review"),
                Map.entry("TEAM_TRANSFER_CAPTAIN", "Transfer team captain ownership")
        ));
        permissions.putAll(PortalPagePermissionCatalog.permissionDescriptions());

        permissions.forEach((code, description) -> {
            if (!adminPermissionRepository.existsByCode(code)) {
                adminPermissionRepository.save(AdminPermission.builder()
                        .code(code)
                        .name(labelFromCode(code))
                        .module(moduleFromCode(code))
                        .description(description)
                        .build());
            }
        });

        createRoleIfMissing("SUPER_ADMIN", "Full platform access", permissions.keySet());
        createRoleIfMissing("PLATFORM_ADMIN", "Manage users, owners, bookings, turfs, and reports", Set.of(
                "USER_VIEW", "USER_EDIT", "USER_SUSPEND", "USER_WARN", "OWNER_APPROVE", "OWNER_REJECT", "TURF_MODERATE", "BOOKING_VIEW", "BOOKING_EDIT", "BOOKING_CANCEL", "DISPUTE_RESOLVE", "REFUND_OVERRIDE", "ESCALATION_MANAGE", "REPORT_VIEW", "ANALYTICS_VIEW", "REVENUE_ANALYTICS_VIEW", "FORECAST_VIEW", "AI_INSIGHTS_VIEW", "CAMPAIGN_CREATE", "COUPON_CREATE", "SYSTEM_CONFIG_VIEW", "FEATURE_FLAG_EDIT", "REPORT_RESOLVE", "AUDIT_VIEW", "PERMISSION_VIEW"
        ));
        createRoleIfMissing("FINANCE_ADMIN", "Manage payments, refunds, wallets, settlements, and finance reports", Set.of(
                "PAYMENT_VIEW", "PAYMENT_EDIT", "PAYMENT_REFUND", "PAYMENT_PAYOUT", "PAYMENT_SETTLEMENT", "REFUND_APPROVE", "SETTLEMENT_PROCESS", "WALLET_ADJUST", "COMMISSION_EDIT", "BOOKING_VIEW", "REPORT_VIEW", "ANALYTICS_VIEW", "REVENUE_ANALYTICS_VIEW", "EXPORT_REPORTS", "AUDIT_VIEW"
        ));
        createRoleIfMissing("SUPPORT_ADMIN", "Manage support tickets, user reports, and escalations", Set.of(
                "USER_VIEW", "USER_WARN", "BOOKING_VIEW", "DISPUTE_RESOLVE", "ESCALATION_MANAGE", "SUPPORT_MANAGE", "TICKET_VIEW", "TICKET_ASSIGN", "TICKET_RESOLVE", "CHAT_ACCESS", "FAQ_EDIT", "KB_EDIT", "CSAT_VIEW", "AUDIT_VIEW", "REPORT_VIEW", "REPORT_RESOLVE", "TRUST_MODERATE", "APPEAL_REVIEW"
        ));
        createRoleIfMissing("CONTENT_ADMIN", "Moderate community, media, CMS, and tournaments", Set.of(
                "COMMUNITY_MODERATE", "MEDIA_MODERATE", "TEAM_MODERATE", "REVIEW_MODERATE", "CMS_MANAGE", "CMS_EDIT", "CAMPAIGN_CREATE", "EMAIL_SEND", "PUSH_SEND", "SEO_EDIT", "TOURNAMENT_MANAGE", "REPORT_VIEW"
        ));
        createRoleIfMissing("OPERATIONS_ADMIN", "Manage owners, turf approvals, trust, and operational queues", Set.of(
                "USER_VIEW", "USER_SUSPEND", "USER_WARN", "OWNER_APPROVE", "OWNER_REJECT", "OWNER_STAFF_MANAGE", "TURF_MODERATE", "TRUST_MODERATE", "TRUST_EDIT", "BOOKING_VIEW", "BOOKING_EDIT", "BOOKING_CANCEL", "DISPUTE_RESOLVE", "ESCALATION_MANAGE", "REPORT_VIEW", "REPORT_RESOLVE"
        ));
        createRoleIfMissing("OWNER_MANAGER", "Owner staff manager role", Set.of("BOOKING_VIEW", "BOOKING_EDIT", "OWNER_STAFF_MANAGE", "REPORT_VIEW"));
        createRoleIfMissing("OWNER_RECEPTIONIST", "Owner front desk role", Set.of("BOOKING_VIEW"));
        createRoleIfMissing("OWNER_FINANCE", "Owner finance staff role", Set.of("PAYMENT_VIEW", "REPORT_VIEW"));
        seedPermissionGroup("User Management", "Player, owner, and team account controls", Set.of("USER_VIEW", "USER_EDIT", "USER_SUSPEND", "USER_DELETE", "TEAM_DELETE", "TEAM_TRANSFER_CAPTAIN"));
        seedPermissionGroup("Booking Management", "Booking visibility, overrides, cancellations, disputes, refunds, and escalations", Set.of("BOOKING_VIEW", "BOOKING_EDIT", "BOOKING_CANCEL", "DISPUTE_RESOLVE", "REFUND_OVERRIDE", "ESCALATION_MANAGE"));
        seedPermissionGroup("Finance Operations", "Transactions, refunds, payouts, wallets, commissions, and settlements", Set.of("PAYMENT_VIEW", "PAYMENT_EDIT", "PAYMENT_REFUND", "PAYMENT_PAYOUT", "PAYMENT_SETTLEMENT", "REFUND_APPROVE", "SETTLEMENT_PROCESS", "WALLET_ADJUST", "COMMISSION_EDIT"));
        seedPermissionGroup("Platform Analytics", "Executive KPIs, revenue analytics, forecasts, AI insights, and report exports", Set.of("ANALYTICS_VIEW", "REVENUE_ANALYTICS_VIEW", "FORECAST_VIEW", "AI_INSIGHTS_VIEW", "EXPORT_REPORTS", "REPORT_VIEW"));
        seedPermissionGroup("Marketing Growth", "CMS, banners, coupons, campaigns, email, push, announcements, landing pages, and SEO", Set.of("CMS_MANAGE", "CMS_EDIT", "COUPON_CREATE", "CAMPAIGN_CREATE", "EMAIL_SEND", "PUSH_SEND", "SEO_EDIT"));
        seedPermissionGroup("Customer Support", "Tickets, live chat, escalations, agents, knowledge base, FAQs, and CSAT", Set.of("SUPPORT_MANAGE", "TICKET_VIEW", "TICKET_ASSIGN", "TICKET_RESOLVE", "CHAT_ACCESS", "FAQ_EDIT", "KB_EDIT", "CSAT_VIEW"));
        seedPermissionGroup("Trust & Safety", "Reports, warnings, suspensions, bans, appeals, media, reviews, teams, and trust score controls", Set.of("REPORT_VIEW", "REPORT_RESOLVE", "USER_WARN", "USER_SUSPEND", "USER_BAN", "TRUST_MODERATE", "TRUST_EDIT", "MEDIA_MODERATE", "APPEAL_REVIEW", "TEAM_MODERATE", "REVIEW_MODERATE", "COMMUNITY_MODERATE"));
        seedPermissionGroup("Platform Security", "RBAC, policies, resource permissions, temporary access, and audits", Set.of("ROLE_MANAGE", "PERMISSION_VIEW", "PERMISSION_EDIT", "POLICY_MANAGE", "RESOURCE_PERMISSION_MANAGE", "TEMPORARY_ACCESS_GRANT", "AUDIT_VIEW", "SECURITY_AUDIT_VIEW"));
        seedPermissionGroup("System Administration", "Global settings, feature flags, business rules, maintenance, releases, and configuration audits", Set.of("SYSTEM_CONFIG_VIEW", "SYSTEM_CONFIG_EDIT", "FEATURE_FLAG_EDIT", "MAINTENANCE_CONTROL", "RELEASE_MANAGE", "AUDIT_VIEW", "SYSTEM_SETTINGS_EDIT"));
        seedPermissionGroup("Owner Staff", "Owner staff roles and turf operations", Set.of("OWNER_STAFF_MANAGE", "BOOKING_VIEW", "TURF_MODERATE", "REPORT_VIEW"));
        seedPermissionGroup("Player Portal Pages", "Read, write, and full access controls for player portal pages", PortalPagePermissionCatalog.pages().stream()
                .filter(page -> "PLAYER".equals(page.portal()))
                .flatMap(page -> Set.of(page.readCode(), page.writeCode(), page.fullCode()).stream())
                .toList());
        seedPermissionGroup("Owner Portal Pages", "Read, write, and full access controls for owner portal pages", PortalPagePermissionCatalog.pages().stream()
                .filter(page -> "OWNER".equals(page.portal()))
                .flatMap(page -> Set.of(page.readCode(), page.writeCode(), page.fullCode()).stream())
                .toList());
        seedPermissionGroup("Admin Portal Pages", "Read, write, and full access controls for admin portal pages", PortalPagePermissionCatalog.pages().stream()
                .filter(page -> "ADMIN".equals(page.portal()))
                .flatMap(page -> Set.of(page.readCode(), page.writeCode(), page.fullCode()).stream())
                .toList());
    }

    private void createRoleIfMissing(String name, String description, Collection<String> permissionCodes) {
        if (adminRoleRepository.findByName(name).isPresent()) {
            AdminRole role = adminRoleRepository.findByName(name).get();
            Set<AdminPermission> permissions = new HashSet<>(role.getPermissions());
            permissions.addAll(adminPermissionRepository.findByCodeIn(permissionCodes));
            role.setPermissions(permissions);
            role.setDescription(description);
            role.setSystemRole(true);
            role.setActive(true);
            adminRoleRepository.save(role);
            return;
        }
        AdminRole role = AdminRole.builder()
                .name(name)
                .description(description)
                .systemRole(true)
                .active(true)
                .permissions(new HashSet<>(adminPermissionRepository.findByCodeIn(permissionCodes)))
                .build();
        adminRoleRepository.save(role);
    }

    private void seedPermissionGroup(String name, String description, Collection<String> permissionCodes) {
        if (permissionGroupRepository.findByName(name).isPresent()) {
            return;
        }
        permissionGroupRepository.save(PermissionGroup.builder()
                .name(name)
                .description(description)
                .permissions(new HashSet<>(adminPermissionRepository.findByCodeIn(permissionCodes)))
                .build());
    }

    private String moduleFromCode(String code) {
        int index = code.indexOf("_");
        return index > 0 ? code.substring(0, index) : "SYSTEM";
    }

    private String labelFromCode(String code) {
        String lower = code.toLowerCase().replace("_", " ");
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
