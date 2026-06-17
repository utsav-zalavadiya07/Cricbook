package com.example.Cricbook.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PortalPagePermissionCatalog {

    private PortalPagePermissionCatalog() {
    }

    public record PortalPage(String portal, String route, String label, String description, String key) {
        public String readCode() {
            return key + "_READ";
        }

        public String writeCode() {
            return key + "_WRITE";
        }

        public String fullCode() {
            return key + "_FULL";
        }
    }

    public static List<PortalPage> pages() {
        return List.of(
                page("PLAYER", "/search", "Search Turfs", "Find turfs and filter availability", "PLAYER_SEARCH"),
                page("PLAYER", "/booking/history", "My Bookings", "View booking history, payment state, and actions", "PLAYER_BOOKINGS"),
                page("PLAYER", "/rebook", "Rebook", "Repeat previous bookings quickly", "PLAYER_REBOOK"),
                page("PLAYER", "/favorites", "Favorites", "Saved turf and match preferences", "PLAYER_FAVORITES"),
                page("PLAYER", "/teams", "Teams", "Create, join, and manage player teams", "PLAYER_TEAMS"),
                page("PLAYER", "/social", "Community", "Community network and connections", "PLAYER_COMMUNITY"),
                page("PLAYER", "/feed", "Feed", "Social feed and activity events", "PLAYER_FEED"),
                page("PLAYER", "/wallet", "Wallet", "Wallet balance, credits, and transactions", "PLAYER_WALLET"),
                page("PLAYER", "/split-payments", "Split Payments", "Shared booking expense collection", "PLAYER_SPLIT_PAYMENTS"),
                page("PLAYER", "/weather", "Weather", "Weather policy and booking impact", "PLAYER_WEATHER"),
                page("PLAYER", "/tournaments", "Tournaments", "Tournament discovery and registration", "PLAYER_TOURNAMENTS"),
                page("PLAYER", "/live-scoring", "Live Scoring", "Match scoring and scoreboard tools", "PLAYER_LIVE_SCORING"),
                page("PLAYER", "/progress", "Progress", "Achievements, milestones, and gamification", "PLAYER_PROGRESS"),
                page("PLAYER", "/personalized", "For You", "Personalized turf and match recommendations", "PLAYER_FOR_YOU"),
                page("PLAYER", "/marketplace", "Marketplace", "Player marketplace listings and offers", "PLAYER_MARKETPLACE"),
                page("PLAYER", "/rewards", "Rewards", "Rewards and loyalty benefits", "PLAYER_REWARDS"),
                page("PLAYER", "/referrals", "Referrals", "Referral invites and rewards", "PLAYER_REFERRALS"),
                page("PLAYER", "/pricing", "Pricing", "Plan and pricing visibility", "PLAYER_PRICING"),
                page("PLAYER", "/subscription", "Subscription", "Subscription plan management", "PLAYER_SUBSCRIPTION"),
                page("PLAYER", "/trust", "Trust", "Trust score and safety record", "PLAYER_TRUST"),
                page("PLAYER", "/smart", "Smart AI", "Smart assistant and recommendations", "PLAYER_SMART"),
                page("PLAYER", "/notifications", "Notifications", "Notification inbox and read controls", "PLAYER_NOTIFICATIONS"),
                page("PLAYER", "/profile", "Profile", "Player profile and media", "PLAYER_PROFILE"),

                page("OWNER", "/owner/dashboard", "Dashboard", "Owner business overview and KPIs", "OWNER_DASHBOARD"),
                page("OWNER", "/owner/slots", "Slot Manager", "Slot availability, price, and block controls", "OWNER_SLOTS"),
                page("OWNER", "/owner/bookings", "Bookings", "Owner booking operations and customer handling", "OWNER_BOOKINGS"),
                page("OWNER", "/turf/my-turfs", "My Turfs", "Turf inventory and listing management", "OWNER_TURFS"),
                page("OWNER", "/owner/earnings", "Earnings", "Revenue and payout visibility", "OWNER_EARNINGS"),
                page("OWNER", "/owner/reviews", "Reviews", "Turf review management", "OWNER_REVIEWS"),
                page("OWNER", "/owner/pricing", "Pricing", "Dynamic pricing and pricing rules", "OWNER_PRICING"),
                page("OWNER", "/owner/payment-rules", "Payment Rules", "Owner payment policies and rules", "OWNER_PAYMENT_RULES"),
                page("OWNER", "/owner/insights", "Insights", "Owner performance analytics", "OWNER_INSIGHTS"),
                page("OWNER", "/owner/booking-intelligence", "Booking Intelligence", "Recurring customers and booking intelligence", "OWNER_BOOKING_INTELLIGENCE"),
                page("OWNER", "/owner/community", "Community", "Owner community and team engagement", "OWNER_COMMUNITY"),
                page("OWNER", "/owner/teams", "Teams", "Top teams and owner team relationships", "OWNER_TEAMS"),
                page("OWNER", "/owner/weather", "Weather Policy", "Weather-aware booking policies", "OWNER_WEATHER"),
                page("OWNER", "/owner/resale", "Resale Rules", "Booking resale and waitlist policy", "OWNER_RESALE"),
                page("OWNER", "/owner/engagement", "Engagement", "Customer engagement analytics", "OWNER_ENGAGEMENT"),
                page("OWNER", "/owner/trust", "Trust Rules", "Trust and safety controls for owners", "OWNER_TRUST"),
                page("OWNER", "/owner/subscription", "Subscription", "Owner plan, featured listing, and billing controls", "OWNER_SUBSCRIPTION"),
                page("OWNER", "/owner/settings", "Settings", "Owner account and operational preferences", "OWNER_SETTINGS"),

                page("ADMIN", "/admin", "Admin Dashboard", "Executive command center", "ADMIN_DASHBOARD"),
                page("ADMIN", "/admin/users", "Users & Owners", "User and owner management center", "ADMIN_USERS"),
                page("ADMIN", "/admin/turfs", "Turfs", "Turf moderation and status control", "ADMIN_TURFS"),
                page("ADMIN", "/admin/bookings", "Bookings", "Booking operations and dispute resolution", "ADMIN_BOOKINGS"),
                page("ADMIN", "/admin/finance", "Finance", "Finance operations center", "ADMIN_FINANCE"),
                page("ADMIN", "/admin/safety", "Trust & Safety", "Community moderation and safety controls", "ADMIN_SAFETY"),
                page("ADMIN", "/admin/analytics", "Analytics", "Platform BI and executive analytics", "ADMIN_ANALYTICS"),
                page("ADMIN", "/admin/ai", "AI Intelligence", "AI assistant, recommendations, forecasts, fraud, churn, and governance", "ADMIN_AI"),
                page("ADMIN", "/admin/sports", "Sports OS", "Multi-sport catalog, rules, scoring, rankings, and no-code sport onboarding", "ADMIN_SPORTS"),
                page("ADMIN", "/admin/developers", "Developer Platform", "API marketplace, OAuth, API keys, webhooks, sandbox, billing, and partner ecosystem", "ADMIN_DEVELOPERS"),
                page("ADMIN", "/admin/academy", "Academy & Talent", "Academies, coaches, training, attendance, assessments, certificates, and talent discovery", "ADMIN_ACADEMY"),
                page("ADMIN", "/admin/corporate", "Corporate Sports", "Company accounts, employees, teams, wellness, rewards, billing, leagues, and B2B CRM", "ADMIN_CORPORATE"),
                page("ADMIN", "/admin/expansion", "Enterprise", "Multi-center franchise and staff control", "ADMIN_ENTERPRISE"),
                page("ADMIN", "/admin/workforce", "Workforce", "Staff mobile operations and workforce execution", "ADMIN_WORKFORCE"),
                page("ADMIN", "/admin/marketing", "Marketing", "CMS, campaigns, and promotions", "ADMIN_MARKETING"),
                page("ADMIN", "/admin/monetization", "Monetization", "Sponsorship, advertising, brand partnerships, leads, contracts, and billing", "ADMIN_MONETIZATION"),
                page("ADMIN", "/admin/support", "Support", "Helpdesk, SLA, and ticket operations", "ADMIN_SUPPORT"),
                page("ADMIN", "/admin/roles", "Roles & Permissions", "RBAC role and permission management", "ADMIN_ROLES"),
                page("ADMIN", "/admin/system", "System", "Feature flags, settings, releases, and maintenance", "ADMIN_SYSTEM")
        );
    }

    public static Map<String, String> permissionDescriptions() {
        Map<String, String> permissions = new LinkedHashMap<>();
        for (PortalPage page : pages()) {
            permissions.put(page.readCode(), "Read-only access to " + page.portal() + " portal page: " + page.label());
            permissions.put(page.writeCode(), "Write action access to " + page.portal() + " portal page: " + page.label());
            permissions.put(page.fullCode(), "Full read/write/control access to " + page.portal() + " portal page: " + page.label());
        }
        return permissions;
    }

    public static List<Map<String, Object>> catalog() {
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (PortalPage page : pages()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("portal", page.portal());
            item.put("route", page.route());
            item.put("label", page.label());
            item.put("description", page.description());
            item.put("key", page.key());
            item.put("permissions", Map.of(
                    "read", page.readCode(),
                    "write", page.writeCode(),
                    "full", page.fullCode()
            ));
            catalog.add(item);
        }
        return catalog;
    }

    private static PortalPage page(String portal, String route, String label, String description, String key) {
        return new PortalPage(portal, route, label, description, key);
    }
}
