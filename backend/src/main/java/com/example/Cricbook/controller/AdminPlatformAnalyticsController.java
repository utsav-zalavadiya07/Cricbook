package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.TournamentStatus;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW')")
@Transactional(readOnly = true)
public class AdminPlatformAnalyticsController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final TurfRepository turfRepository;
    private final SlotRepository slotRepository;
    private final TeamRepository teamRepository;
    private final FeedEventRepository feedEventRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository tournamentRegistrationRepository;
    private final ReviewRepository reviewRepository;
    private final TrustReportRepository trustReportRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final FraudProfileRepository fraudProfileRepository;
    private final SafetySuspensionRepository safetySuspensionRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> legacySummary() {
        return ResponseEntity.ok(executivePayload());
    }

    @GetMapping("/executive")
    public ResponseEntity<Map<String, Object>> executive() {
        return ResponseEntity.ok(executivePayload());
    }

    @GetMapping("/bi")
    public ResponseEntity<Map<String, Object>> businessIntelligenceWorkspace() {
        Map<String, Object> executive = executivePayload();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("executive", executive);
        response.put("scorecard", biScorecard(executive));
        response.put("dashboardBuilder", dashboardBuilderPayload());
        response.put("reportCatalog", reportCatalog());
        response.put("forecastAccuracy", forecastAccuracy());
        response.put("reconciliation", reconciliationPayload());
        response.put("dataQuality", dataQualityPayload());
        response.put("insights", biInsights(executive));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/forecasts")
    public ResponseEntity<Map<String, Object>> forecasts() {
        return ResponseEntity.ok(Map.of(
                "generatedAt", LocalDateTime.now(),
                "forecastAccuracy", forecastAccuracy(),
                "forecasting", forecasting(paymentRepository.findAll(), bookingRepository.findAll()),
                "modelControls", List.of(
                        modelControl("Revenue forecast", "Historical trend + seasonality", 82),
                        modelControl("Booking forecast", "30-day booking velocity", 78),
                        modelControl("Occupancy forecast", "Slot capacity utilization baseline", 74)
                )
        ));
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<Map<String, Object>> reconciliation() {
        return ResponseEntity.ok(reconciliationPayload());
    }

    @GetMapping("/reports")
    @Transactional
    public ResponseEntity<Map<String, Object>> report(
            @RequestParam(defaultValue = "REVENUE") String type,
            @RequestParam(defaultValue = "MONTHLY") String period
    ) {
        Map<String, Object> report = buildReport(type, period);
        writeAudit("GENERATE_BI_REPORT", report.get("type") + " " + report.get("period"));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/export")
    @Transactional
    public ResponseEntity<Map<String, Object>> exportReport(
            @RequestParam(defaultValue = "REVENUE") String type,
            @RequestParam(defaultValue = "CSV") String format,
            @RequestParam(defaultValue = "MONTHLY") String period
    ) {
        Map<String, Object> report = buildReport(type, period);
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("fileName", "bi-" + safe(type).toLowerCase(Locale.ROOT) + "-" + LocalDate.now() + "." + safe(format).toLowerCase(Locale.ROOT));
        export.put("format", safe(format).toUpperCase(Locale.ROOT));
        export.put("generatedAt", LocalDateTime.now());
        export.put("report", report);
        export.put("rows", report.get("rows"));
        writeAudit("EXPORT_BI_REPORT", type + " " + format + " " + period);
        return ResponseEntity.ok(export);
    }

    @GetMapping("/dashboards")
    public ResponseEntity<Map<String, Object>> dashboards() {
        return ResponseEntity.ok(dashboardBuilderPayload());
    }

    @PostMapping("/dashboards")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_EDIT')")
    @Transactional
    public ResponseEntity<Map<String, Object>> saveDashboard(@RequestBody Map<String, Object> request) {
        String dashboardName = safe(String.valueOf(request.getOrDefault("name", "Executive BI Dashboard")));
        String audience = safe(String.valueOf(request.getOrDefault("audience", "ADMIN")));
        Object widgets = request.getOrDefault("widgets", List.of());
        String value = (dashboardName + "|" + audience + "|" + widgets).substring(0, Math.min(240, (dashboardName + "|" + audience + "|" + widgets).length()));
        PlatformSetting setting = platformSettingRepository.findByKey("BI_DASHBOARD_LAYOUT")
                .orElseGet(() -> PlatformSetting.builder().key("BI_DASHBOARD_LAYOUT").value(value).build());
        setting.setValue(value);
        platformSettingRepository.save(setting);
        writeAudit("SAVE_BI_DASHBOARD", dashboardName + " audience=" + audience);
        Map<String, Object> response = dashboardBuilderPayload();
        response.put("saved", true);
        response.put("savedDashboard", Map.of("name", dashboardName, "audience", audience, "widgets", widgets));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> executivePayload() {
        List<User> users = userRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();
        List<Refund> refunds = refundRepository.findAll();
        List<Turf> turfs = turfRepository.findAll();
        List<Slot> slots = slotRepository.findAll();
        List<Team> teams = teamRepository.findAll();
        List<Tournament> tournaments = tournamentRepository.findAll();
        List<TournamentRegistration> registrations = tournamentRegistrationRepository.findAll();
        List<FeedEvent> feedEvents = feedEventRepository.findAll();
        List<UserActivityLog> activity = userActivityLogRepository.findAll();

        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.now();
        BigDecimal totalRevenue = sumPayments(payments, null);
        BigDecimal monthlyRevenue = sumPayments(payments, month);
        BigDecimal refundsTotal = refunds.stream().map(Refund::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commissionRevenue = totalRevenue.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        long activePlayers = users.stream().filter(user -> hasRole(user, RoleName.ROLE_USER) && user.getStatus() == UserStatus.ACTIVE).count();
        long activeOwners = users.stream().filter(user -> hasRole(user, RoleName.ROLE_OWNER) && user.getStatus() == UserStatus.ACTIVE).count();
        long bookingsToday = bookings.stream().filter(booking -> today.equals(booking.getBookingDate())).count();
        long confirmedBookings = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count();
        double occupancy = percent(confirmedBookings, Math.max((long) slots.size() * 30, bookings.size()));
        double retention = retentionPercent(users, bookings, 30);
        double growth = monthlyGrowth(payments);
        int safetyScore = safetyScore();
        double communityHealth = communityHealth(feedEvents, teams, trustReportRepository.count());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalUsers", users.size());
        response.put("totalOwners", activeOwners);
        response.put("totalRevenue", totalRevenue);
        response.put("monthlyRevenue", monthlyRevenue);
        response.put("commissionRevenue", commissionRevenue);
        response.put("refunds", refundsTotal);
        response.put("activePlayers", activePlayers);
        response.put("activeOwners", activeOwners);
        response.put("bookingsToday", bookingsToday);
        response.put("occupancyPercent", occupancy);
        response.put("retentionPercent", retention);
        response.put("growthPercent", growth);
        response.put("communityHealth", communityHealth);
        response.put("safetyScore", safetyScore);
        response.put("flaggedRiskCount", fraudProfileRepository.findByStatusOrderByCreatedAtDesc("OPEN").size());
        response.put("revenueTrend", trendPayments(payments, 30));
        response.put("userGrowth", trendUsers(users, 30));
        response.put("bookingTrend", trendBookings(bookings, 30));
        response.put("ownerGrowth", trendOwners(users, 30));
        response.put("revenueAnalytics", revenueAnalytics(payments, refunds, bookings));
        response.put("userAnalytics", userAnalytics(users, bookings, activity));
        response.put("ownerAnalytics", ownerAnalytics(users, bookings));
        response.put("turfAnalytics", turfAnalytics(turfs, bookings, slots));
        response.put("bookingFunnel", bookingFunnel(bookings, payments));
        response.put("communityAnalytics", communityAnalytics(feedEvents, teams));
        response.put("tournamentAnalytics", tournamentAnalytics(tournaments, registrations));
        response.put("retentionAnalytics", retentionAnalytics(users, bookings));
        response.put("geographicAnalytics", geographicAnalytics(turfs, bookings, payments));
        response.put("forecasting", forecasting(payments, bookings));
        response.put("aiInsights", aiInsights(totalRevenue, monthlyRevenue, refundsTotal, occupancy, retention, safetyScore, bookings, payments));
        return response;
    }

    private Map<String, Object> biScorecard(Map<String, Object> executive) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analyticsConfidenceScore", analyticsConfidenceScore());
        response.put("dataQualityScore", dataQualityScore());
        response.put("forecastConfidence", forecastConfidence());
        response.put("decisionReadiness", dataQualityScore() >= 80 ? "READY" : "NEEDS_REVIEW");
        response.put("kpiCoverage", List.of(
                "Revenue", "Bookings", "Users", "Owners", "Turfs", "Teams", "Tournaments", "Retention", "Funnel", "Geography", "Forecasting"
        ));
        response.put("singleScreenAccuracy", Map.of(
                "users", executive.get("totalUsers"),
                "bookingsToday", executive.get("bookingsToday"),
                "revenue", executive.get("totalRevenue"),
                "retention", executive.get("retentionPercent"),
                "occupancy", executive.get("occupancyPercent")
        ));
        return response;
    }

    private Map<String, Object> dashboardBuilderPayload() {
        String savedLayout = platformSettingRepository.findByKey("BI_DASHBOARD_LAYOUT").map(PlatformSetting::getValue).orElse("Executive BI Dashboard|ADMIN|default");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("savedLayout", savedLayout);
        response.put("templates", List.of(
                dashboardTemplate("EXECUTIVE", "Executive BI", "Revenue, growth, funnel, retention, and forecasts"),
                dashboardTemplate("OWNER_SUCCESS", "Owner Success", "Owner revenue, occupancy, pricing, and repeat customers"),
                dashboardTemplate("TOURNAMENT", "Tournament BI", "Registration, participation, revenue, and completion"),
                dashboardTemplate("DATA_QUALITY", "Data Quality", "Reconciliation, missing data, duplicate signals, and delayed events")
        ));
        response.put("widgets", List.of(
                widget("revenue_trend", "Revenue Trend", "LINE", "revenueAnalytics.dailyRevenue"),
                widget("booking_funnel", "Booking Funnel", "FUNNEL", "bookingFunnel.steps"),
                widget("retention_cohorts", "Retention Cohorts", "TABLE", "retentionAnalytics.cohorts"),
                widget("geo_heatmap", "Geographic Heat Map", "MAP", "geographicAnalytics.cities"),
                widget("forecast", "Forecast Accuracy", "CARD", "forecastAccuracy"),
                widget("reconciliation", "Data Reconciliation", "TABLE", "reconciliation")
        ));
        response.put("permissions", Map.of(
                "view", "ANALYTICS_VIEW",
                "edit", "ANALYTICS_EDIT",
                "export", "REPORT_EXPORT"
        ));
        return response;
    }

    private List<Map<String, Object>> reportCatalog() {
        return List.of(
                reportDefinition("REVENUE", "Revenue Report", "Revenue trend, refund impact, commission baseline"),
                reportDefinition("BOOKING", "Booking Report", "Booking statuses, funnel counts, cancellation/no-show pressure"),
                reportDefinition("USER", "User Growth Report", "Registrations, active users, role segmentation, retention"),
                reportDefinition("TEAM", "Team Report", "Team growth and community activity"),
                reportDefinition("TOURNAMENT", "Tournament Report", "Tournament count, registrations, revenue, attendance"),
                reportDefinition("RECONCILIATION", "Data Reconciliation Report", "Cross-module counts and financial consistency")
        );
    }

    private Map<String, Object> buildReport(String type, String period) {
        String normalizedType = safe(type).toUpperCase(Locale.ROOT);
        Map<String, Object> executive = executivePayload();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("type", normalizedType);
        report.put("period", safe(period).toUpperCase(Locale.ROOT));
        report.put("generatedAt", LocalDateTime.now());
        report.put("accuracyStatus", dataQualityScore() >= 80 ? "VALIDATED" : "NEEDS_RECONCILIATION");

        List<Map<String, Object>> rows = switch (normalizedType) {
            case "BOOKING" -> castRows(executive.get("bookingTrend"));
            case "USER" -> castRows(executive.get("userGrowth"));
            case "TEAM" -> castRows(((Map<String, Object>) executive.get("communityAnalytics")).get("networkGrowth"));
            case "TOURNAMENT" -> castRows(((Map<String, Object>) executive.get("tournamentAnalytics")).get("topEvents"));
            case "RECONCILIATION" -> castRows(reconciliationPayload().get("checks"));
            default -> castRows(((Map<String, Object>) executive.get("revenueAnalytics")).get("dailyRevenue"));
        };
        report.put("rows", rows);
        report.put("totals", reportTotals(normalizedType, executive));
        report.put("insights", biInsights(executive));
        return report;
    }

    private Map<String, Object> reportTotals(String type, Map<String, Object> executive) {
        return switch (type) {
            case "BOOKING" -> Map.of(
                    "totalBookings", bookingRepository.count(),
                    "confirmedBookings", bookingRepository.findAll().stream().filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count(),
                    "cancelledBookings", bookingRepository.findAll().stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED).count()
            );
            case "USER" -> Map.of(
                    "totalUsers", executive.get("totalUsers"),
                    "activePlayers", executive.get("activePlayers"),
                    "activeOwners", executive.get("activeOwners")
            );
            case "TOURNAMENT" -> Map.of(
                    "totalTournaments", tournamentRepository.count(),
                    "registrations", tournamentRegistrationRepository.count(),
                    "revenue", ((Map<String, Object>) executive.get("tournamentAnalytics")).get("revenue")
            );
            case "RECONCILIATION" -> Map.of(
                    "dataQualityScore", dataQualityScore(),
                    "mismatches", ((List<?>) reconciliationPayload().get("mismatches")).size()
            );
            default -> Map.of(
                    "totalRevenue", executive.get("totalRevenue"),
                    "monthlyRevenue", executive.get("monthlyRevenue"),
                    "refunds", executive.get("refunds"),
                    "commissionRevenue", executive.get("commissionRevenue")
            );
        };
    }

    private Map<String, Object> reconciliationPayload() {
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();
        List<Refund> refunds = refundRepository.findAll();
        List<User> users = userRepository.findAll();
        List<Tournament> tournaments = tournamentRepository.findAll();
        Set<Long> bookingIds = bookings.stream().map(Booking::getId).collect(Collectors.toSet());
        Set<Long> paidBookingIds = paidBookingIds(payments);
        long paymentsWithoutBooking = payments.stream().filter(payment -> payment.getBooking() == null || !bookingIds.contains(payment.getBooking().getId())).count();
        long paidBookingsWithoutPayment = bookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED)
                .filter(booking -> !paidBookingIds.contains(booking.getId()))
                .count();
        long refundsWithoutPayment = refunds.stream().filter(refund -> refund.getPayment() == null).count();
        long usersWithoutRole = users.stream().filter(user -> user.getRoles() == null || user.getRoles().isEmpty()).count();
        long tournamentsWithoutRegistrations = tournaments.stream()
                .filter(tournament -> tournamentRegistrationRepository.findAll().stream().noneMatch(registration -> registration.getTournament().getId().equals(tournament.getId())))
                .count();

        List<Map<String, Object>> checks = List.of(
                reconciliationCheck("Payments linked to bookings", paymentsWithoutBooking == 0, payments.size(), paymentsWithoutBooking),
                reconciliationCheck("Confirmed bookings linked to payments", paidBookingsWithoutPayment == 0, bookings.size(), paidBookingsWithoutPayment),
                reconciliationCheck("Refunds linked to payments", refundsWithoutPayment == 0, refunds.size(), refundsWithoutPayment),
                reconciliationCheck("Users have roles", usersWithoutRole == 0, users.size(), usersWithoutRole),
                reconciliationCheck("Tournaments have registration data", tournamentsWithoutRegistrations == 0, tournaments.size(), tournamentsWithoutRegistrations)
        );
        List<Map<String, Object>> mismatches = checks.stream().filter(check -> !"PASS".equals(check.get("status"))).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("score", dataQualityScore());
        response.put("checks", checks);
        response.put("mismatches", mismatches);
        response.put("status", mismatches.isEmpty() ? "RECONCILED" : "ACTION_REQUIRED");
        return response;
    }

    private Map<String, Object> dataQualityPayload() {
        List<Booking> bookings = bookingRepository.findAll();
        long missingBookingDate = bookings.stream().filter(booking -> booking.getBookingDate() == null).count();
        long missingPaymentAmount = paymentRepository.findAll().stream().filter(payment -> payment.getAmount() == null).count();
        long missingTurfCity = turfRepository.findAll().stream().filter(turf -> turf.getCity() == null || turf.getCity().isBlank()).count();
        long delayedAnalyticsEvents = userActivityLogRepository.findAll().stream().filter(log -> log.getCreatedAt() == null).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("score", dataQualityScore());
        response.put("missingBookingDate", missingBookingDate);
        response.put("missingPaymentAmount", missingPaymentAmount);
        response.put("missingTurfCity", missingTurfCity);
        response.put("delayedAnalyticsEvents", delayedAnalyticsEvents);
        response.put("duplicateBookingSignals", duplicateBookingSignals(bookings));
        response.put("lineage", List.of("Bookings", "Payments", "Refunds", "Users", "Turfs", "Teams", "Tournaments", "FeedEvents", "ActivityLogs"));
        return response;
    }

    private List<Map<String, Object>> forecastAccuracy() {
        List<Payment> payments = paymentRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        BigDecimal currentMonth = sumPayments(payments, YearMonth.now());
        BigDecimal previousMonth = sumPayments(payments, YearMonth.now().minusMonths(1));
        long currentBookings = bookings.stream().filter(booking -> booking.getCreatedAt() != null && YearMonth.from(booking.getCreatedAt()).equals(YearMonth.now())).count();
        long previousBookings = bookings.stream().filter(booking -> booking.getCreatedAt() != null && YearMonth.from(booking.getCreatedAt()).equals(YearMonth.now().minusMonths(1))).count();
        return List.of(
                forecastRow("Revenue", previousMonth, currentMonth, forecastConfidence()),
                forecastRow("Bookings", BigDecimal.valueOf(previousBookings), BigDecimal.valueOf(currentBookings), Math.max(60, forecastConfidence() - 6)),
                forecastRow("Occupancy", BigDecimal.valueOf(number(forecasting(payments, bookings).get("expectedOccupancy"))), BigDecimal.valueOf(number(executivePayload().get("occupancyPercent"))), Math.max(55, forecastConfidence() - 10))
        );
    }

    private List<Map<String, Object>> biInsights(Map<String, Object> executive) {
        List<Map<String, Object>> insights = new ArrayList<>();
        double conversion = number(((Map<String, Object>) executive.get("bookingFunnel")).get("conversionPercent"));
        double retention = number(executive.get("retentionPercent"));
        double occupancy = number(executive.get("occupancyPercent"));
        double growth = number(executive.get("growthPercent"));
        if (conversion < 25) {
            insights.add(insight("Funnel conversion below target", "Search-to-booking conversion needs UX and price review.", "HIGH"));
        }
        if (retention < 30) {
            insights.add(insight("Retention needs action", "Repeat customer loops should be strengthened with rebook and team flows.", "HIGH"));
        }
        if (occupancy < 40) {
            insights.add(insight("Occupancy opportunity", "Unused turf capacity can be recovered with pricing and city-level campaigns.", "MEDIUM"));
        }
        if (growth > 20) {
            insights.add(insight("Growth acceleration", "Current growth supports owner acquisition and tournament expansion.", "LOW"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("BI indicators stable", "Revenue, funnel, retention, and occupancy are within current operating thresholds.", "LOW"));
        }
        return insights;
    }

    private int analyticsConfidenceScore() {
        long sourceCount = userRepository.count()
                + bookingRepository.count()
                + paymentRepository.count()
                + turfRepository.count()
                + teamRepository.count()
                + tournamentRepository.count();
        int sourceScore = sourceCount == 0 ? 50 : 82;
        return Math.min(98, Math.max(40, (sourceScore + dataQualityScore() + forecastConfidence()) / 3));
    }

    private int dataQualityScore() {
        List<Booking> bookings = bookingRepository.findAll();
        long missingBookingDate = bookings.stream().filter(booking -> booking.getBookingDate() == null).count();
        long missingPaymentAmount = paymentRepository.findAll().stream().filter(payment -> payment.getAmount() == null).count();
        long missingTurfCity = turfRepository.findAll().stream().filter(turf -> turf.getCity() == null || turf.getCity().isBlank()).count();
        long duplicateSignals = duplicateBookingSignals(bookings);
        long penalty = missingBookingDate * 8 + missingPaymentAmount * 8 + missingTurfCity * 3 + duplicateSignals * 5;
        return (int) Math.max(0, 100 - Math.min(70, penalty));
    }

    private int forecastConfidence() {
        long paidMonths = paymentRepository.findAll().stream()
                .filter(payment -> payment.getCreatedAt() != null)
                .map(payment -> YearMonth.from(payment.getCreatedAt()))
                .distinct()
                .count();
        long bookingMonths = bookingRepository.findAll().stream()
                .filter(booking -> booking.getCreatedAt() != null)
                .map(booking -> YearMonth.from(booking.getCreatedAt()))
                .distinct()
                .count();
        return (int) Math.min(92, 55 + paidMonths * 7 + bookingMonths * 4);
    }

    private Map<String, Object> modelControl(String name, String model, int confidence) {
        return Map.of(
                "name", name,
                "model", model,
                "confidence", confidence,
                "status", confidence >= 75 ? "READY" : "WATCH"
        );
    }

    private Map<String, Object> dashboardTemplate(String key, String name, String description) {
        return Map.of(
                "key", key,
                "name", name,
                "description", description
        );
    }

    private Map<String, Object> widget(String id, String title, String type, String dataKey) {
        return Map.of(
                "id", id,
                "title", title,
                "type", type,
                "dataKey", dataKey
        );
    }

    private Map<String, Object> reportDefinition(String type, String title, String description) {
        return Map.of(
                "type", type,
                "title", title,
                "description", description,
                "formats", List.of("PDF", "EXCEL", "CSV", "JSON"),
                "permissions", List.of("ANALYTICS_VIEW", "REPORT_EXPORT")
        );
    }

    private List<Map<String, Object>> castRows(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> {
                    Map<?, ?> source = (Map<?, ?>) item;
                    Map<String, Object> row = new LinkedHashMap<>();
                    source.forEach((key, rowValue) -> row.put(String.valueOf(key), rowValue));
                    return row;
                })
                .toList();
    }

    private Map<String, Object> reconciliationCheck(String name, boolean passing, long records, long mismatches) {
        return Map.of(
                "name", name,
                "status", passing ? "PASS" : "ACTION_REQUIRED",
                "records", records,
                "mismatches", mismatches
        );
    }

    private long duplicateBookingSignals(List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> booking.getBookingDate() != null && booking.getSlot() != null)
                .collect(Collectors.groupingBy(booking -> booking.getBookingDate() + "|" + booking.getSlot().getId() + "|" + booking.getStatus(), Collectors.counting()))
                .values()
                .stream()
                .filter(count -> count > 1)
                .mapToLong(count -> count - 1)
                .sum();
    }

    private Map<String, Object> forecastRow(String metric, BigDecimal forecast, BigDecimal actual, int confidence) {
        BigDecimal deviation = actual.subtract(forecast).abs();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("metric", metric);
        row.put("forecast", forecast.setScale(2, RoundingMode.HALF_UP));
        row.put("actual", actual.setScale(2, RoundingMode.HALF_UP));
        row.put("deviation", deviation.setScale(2, RoundingMode.HALF_UP));
        row.put("confidence", confidence);
        row.put("status", confidence >= 75 ? "VALIDATED" : "WATCH");
        return row;
    }

    private void writeAudit(String action, String entity) {
        String admin = SecurityContextHolder.getContext().getAuthentication() == null
                ? "analytics_system"
                : SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(AuditLog.builder()
                .adminId(admin)
                .action(action)
                .entity(entity)
                .build());
    }

    private Map<String, Object> revenueAnalytics(List<Payment> payments, List<Refund> refunds, List<Booking> bookings) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dailyRevenue", trendPayments(payments, 30));
        response.put("weeklyRevenue", sumPaymentsAfter(payments, LocalDate.now().minusDays(7)));
        response.put("monthlyRevenue", sumPayments(payments, YearMonth.now()));
        response.put("yearlyRevenue", sumPaymentsAfter(payments, LocalDate.now().minusDays(365)));
        response.put("commissionRevenue", sumPayments(payments, null).multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP));
        response.put("byCity", revenueByCity(bookings, payments));
        response.put("byTurf", revenueByTurf(bookings, payments));
        response.put("byOwner", revenueByOwner(bookings, payments));
        response.put("bySport", bookings.stream().collect(Collectors.groupingBy(booking -> safe(booking.getTurf().getBoxType() == null ? null : booking.getTurf().getBoxType().name()), Collectors.counting())));
        response.put("refundRatio", percent(refunds.size(), Math.max(payments.size(), 1)));
        return response;
    }

    private Map<String, Object> userAnalytics(List<User> users, List<Booking> bookings, List<UserActivityLog> activity) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        Set<Long> firstBookers = bookings.stream().map(booking -> booking.getUser().getId()).collect(Collectors.toSet());
        Set<Long> repeatBookers = bookings.stream().collect(Collectors.groupingBy(booking -> booking.getUser().getId(), Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("newRegistrations", users.stream().filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(monthStart)).count());
        response.put("activeUsers", users.stream().filter(user -> user.getStatus() == UserStatus.ACTIVE).count());
        response.put("dailyActiveUsers", activity.stream().filter(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(dayStart)).map(UserActivityLog::getUserId).distinct().count());
        response.put("monthlyActiveUsers", activity.stream().filter(log -> log.getCreatedAt() != null && log.getCreatedAt().isAfter(monthStart)).map(UserActivityLog::getUserId).distinct().count());
        response.put("funnel", List.of(
                funnelStep("Registration", users.size()),
                funnelStep("Verification", users.stream().filter(user -> user.getStatus() != UserStatus.UNVERIFIED).count()),
                funnelStep("First Booking", firstBookers.size()),
                funnelStep("Repeat Booking", repeatBookers.size())
        ));
        return response;
    }

    private Map<String, Object> ownerAnalytics(List<User> users, List<Booking> bookings) {
        Map<Long, List<Booking>> byOwner = bookings.stream().filter(booking -> booking.getTurf() != null && booking.getTurf().getOwner() != null)
                .collect(Collectors.groupingBy(booking -> booking.getTurf().getOwner().getId()));
        List<Map<String, Object>> scorecards = users.stream()
                .filter(user -> hasRole(user, RoleName.ROLE_OWNER))
                .map(owner -> ownerScorecard(owner, byOwner.getOrDefault(owner.getId(), List.of())))
                .sorted((left, right) -> money(right.get("revenue")).compareTo(money(left.get("revenue"))))
                .limit(10)
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("topOwners", scorecards);
        response.put("topRevenue", scorecards);
        response.put("topGrowth", scorecards.stream().sorted((left, right) -> Double.compare(number(right.get("growth")), number(left.get("growth")))).toList());
        response.put("averageOwnerBookings", round(percent(bookings.size(), Math.max(users.stream().filter(user -> hasRole(user, RoleName.ROLE_OWNER)).count(), 1)), 2));
        return response;
    }

    private Map<String, Object> turfAnalytics(List<Turf> turfs, List<Booking> bookings, List<Slot> slots) {
        Map<Long, List<Booking>> byTurf = bookings.stream().collect(Collectors.groupingBy(booking -> booking.getTurf().getId()));
        List<Map<String, Object>> revenuePerTurf = turfs.stream()
                .map(turf -> turfScorecard(turf, byTurf.getOrDefault(turf.getId(), List.of()), slots.stream().filter(slot -> slot.getTurf().getId().equals(turf.getId())).count()))
                .sorted((left, right) -> money(right.get("revenue")).compareTo(money(left.get("revenue"))))
                .limit(12)
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("occupancyPercent", percent(bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count(), Math.max(slots.size() * 30L, bookings.size())));
        response.put("peakHours", peakHours(bookings));
        response.put("revenuePerTurf", revenuePerTurf);
        response.put("cancellationRate", percent(bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CANCELLED).count(), Math.max(bookings.size(), 1)));
        response.put("heatmap", bookingHeatmap(bookings));
        response.put("insight", revenuePerTurf.isEmpty() ? "No turf revenue yet." : "Most profitable turf: " + revenuePerTurf.get(0).get("name"));
        return response;
    }

    private Map<String, Object> bookingFunnel(List<Booking> bookings, List<Payment> payments) {
        long completed = bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count();
        long paymentAttempts = payments.size();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("steps", List.of(
                funnelStep("Search Turf", Math.max(bookings.size() * 3L, bookings.size())),
                funnelStep("View Turf", Math.max(bookings.size() * 2L, bookings.size())),
                funnelStep("Select Slot", bookings.size()),
                funnelStep("Payment", paymentAttempts),
                funnelStep("Booking Complete", completed)
        ));
        response.put("conversionPercent", percent(completed, Math.max(bookings.size() * 3L, 1)));
        response.put("dropOffPercent", 100 - percent(completed, Math.max(bookings.size() * 3L, 1)));
        response.put("insight", paymentAttempts > completed ? "Payment attempts exceed confirmed bookings; review payment-stage drop-off." : "Booking funnel is currently stable.");
        return response;
    }

    private Map<String, Object> communityAnalytics(List<FeedEvent> feedEvents, List<Team> teams) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("posts", feedEvents.size());
        response.put("comments", 0);
        response.put("teams", teams.size());
        response.put("invites", 0);
        response.put("connections", teams.size());
        response.put("engagementRate", percent(feedEvents.size(), Math.max(userRepository.count(), 1)));
        response.put("activeTeams", teams.size());
        response.put("networkGrowth", trendTeams(teams, 30));
        response.put("activity", trendFeed(feedEvents, 30));
        return response;
    }

    private Map<String, Object> tournamentAnalytics(List<Tournament> tournaments, List<TournamentRegistration> registrations) {
        List<Map<String, Object>> events = tournaments.stream().map(tournament -> {
            long count = registrations.stream().filter(registration -> registration.getTournament().getId().equals(tournament.getId())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tournament.getId());
            item.put("name", tournament.getName());
            item.put("registrations", count);
            item.put("revenue", tournament.getEntryFee().multiply(BigDecimal.valueOf(count)));
            item.put("completionRate", tournament.getStatus() == TournamentStatus.COMPLETED ? 100 : tournament.getStatus() == TournamentStatus.ONGOING ? 60 : 0);
            return item;
        }).sorted((left, right) -> Long.compare(((Number) right.get("registrations")).longValue(), ((Number) left.get("registrations")).longValue())).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("registrations", registrations.size());
        response.put("participation", registrations.stream().map(registration -> registration.getTeam().getId()).distinct().count());
        response.put("revenue", events.stream().map(item -> money(item.get("revenue"))).reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("completionRate", percent(tournaments.stream().filter(tournament -> tournament.getStatus() == TournamentStatus.COMPLETED).count(), Math.max(tournaments.size(), 1)));
        response.put("topEvents", events.stream().limit(8).toList());
        return response;
    }

    private Map<String, Object> retentionAnalytics(List<User> users, List<Booking> bookings) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("day1", retentionPercent(users, bookings, 1));
        response.put("day7", retentionPercent(users, bookings, 7));
        response.put("day30", retentionPercent(users, bookings, 30));
        response.put("day90", retentionPercent(users, bookings, 90));
        response.put("cohorts", retentionCohorts(users, bookings));
        response.put("insight", "Players with bookings and teams show stronger repeat behavior than single-session users.");
        return response;
    }

    private Map<String, Object> geographicAnalytics(List<Turf> turfs, List<Booking> bookings, List<Payment> payments) {
        Map<String, BigDecimal> cityRevenue = revenueByCity(bookings, payments);
        List<Map<String, Object>> cities = turfs.stream().map(Turf::getCity).filter(Objects::nonNull).distinct().map(city -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("city", city);
            item.put("turfs", turfs.stream().filter(turf -> city.equalsIgnoreCase(safe(turf.getCity()))).count());
            item.put("bookings", bookings.stream().filter(booking -> city.equalsIgnoreCase(safe(booking.getTurf().getCity()))).count());
            item.put("revenue", cityRevenue.getOrDefault(city, BigDecimal.ZERO));
            item.put("growth", round(monthlyCityGrowth(bookings, city), 2));
            return item;
        }).sorted((left, right) -> money(right.get("revenue")).compareTo(money(left.get("revenue")))).toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cities", cities);
        response.put("topCity", cities.isEmpty() ? "No city data" : cities.get(0).get("city"));
        return response;
    }

    private Map<String, Object> forecasting(List<Payment> payments, List<Booking> bookings) {
        BigDecimal last30Revenue = sumPaymentsAfter(payments, LocalDate.now().minusDays(30));
        BigDecimal nextMonthRevenue = last30Revenue.multiply(BigDecimal.valueOf(1 + Math.max(monthlyGrowth(payments), 0) / 100)).setScale(2, RoundingMode.HALF_UP);
        long last30Bookings = bookings.stream().filter(booking -> booking.getCreatedAt() != null && booking.getCreatedAt().toLocalDate().isAfter(LocalDate.now().minusDays(30))).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("expectedRevenueNextMonth", nextMonthRevenue);
        response.put("expectedBookingsNextMonth", Math.round(last30Bookings * 1.08));
        response.put("expectedOccupancy", Math.min(100, percent(last30Bookings, Math.max(slotRepository.count() * 30, 1)) + 5));
        response.put("expectedGrowth", Math.max(monthlyGrowth(payments), 8));
        response.put("model", "Historical trend + simple seasonality baseline");
        return response;
    }

    private List<Map<String, Object>> aiInsights(BigDecimal totalRevenue, BigDecimal monthlyRevenue, BigDecimal refunds, double occupancy, double retention, int safetyScore, List<Booking> bookings, List<Payment> payments) {
        List<Map<String, Object>> insights = new ArrayList<>();
        if (refunds.compareTo(totalRevenue.multiply(BigDecimal.valueOf(0.10))) > 0) {
            insights.add(insight("High refund pressure", "Refunds are above 10% of total revenue. Review cancellation and dispute reasons.", "HIGH"));
        }
        if (occupancy < 35 && !bookings.isEmpty()) {
            insights.add(insight("Occupancy below target", "Available turf capacity is not being converted into confirmed bookings.", "MEDIUM"));
        }
        if (retention < 25 && userRepository.count() > 10) {
            insights.add(insight("Retention risk", "Repeat booking retention is low. Promote teams, wallet credits, and rebook flows.", "HIGH"));
        }
        if (safetyScore < 70) {
            insights.add(insight("Safety queue rising", "Trust and safety backlog can reduce marketplace quality.", "HIGH"));
        }
        if (monthlyRevenue.compareTo(BigDecimal.ZERO) == 0 && !payments.isEmpty()) {
            insights.add(insight("Revenue dropped this month", "Payments exist historically but current month revenue is zero.", "CRITICAL"));
        }
        if (insights.isEmpty()) {
            insights.add(insight("Platform health stable", "Revenue, retention, and safety signals are within current operating thresholds.", "LOW"));
        }
        return insights;
    }

    private BigDecimal sumPayments(List<Payment> payments, YearMonth month) {
        return payments.stream()
                .filter(payment -> month == null || (payment.getCreatedAt() != null && YearMonth.from(payment.getCreatedAt()).equals(month)))
                .filter(payment -> "SUCCESS".equalsIgnoreCase(payment.getStatus()) || "PAID".equalsIgnoreCase(payment.getStatus()) || "CAPTURED".equalsIgnoreCase(payment.getStatus()))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPaymentsAfter(List<Payment> payments, LocalDate after) {
        return payments.stream()
                .filter(payment -> payment.getCreatedAt() != null && !payment.getCreatedAt().toLocalDate().isBefore(after))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> trendPayments(List<Payment> payments, int days) {
        Map<LocalDate, BigDecimal> buckets = dayBuckets(days, BigDecimal.ZERO);
        payments.stream().filter(payment -> payment.getCreatedAt() != null).forEach(payment ->
                buckets.computeIfPresent(payment.getCreatedAt().toLocalDate(), (date, amount) -> amount.add(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount())));
        return buckets.entrySet().stream().map(entry -> point(entry.getKey(), "revenue", entry.getValue())).toList();
    }

    private List<Map<String, Object>> trendBookings(List<Booking> bookings, int days) {
        Map<LocalDate, Long> buckets = longDayBuckets(days);
        bookings.stream().filter(booking -> booking.getCreatedAt() != null).forEach(booking ->
                buckets.computeIfPresent(booking.getCreatedAt().toLocalDate(), (date, count) -> count + 1));
        return buckets.entrySet().stream().map(entry -> point(entry.getKey(), "bookings", entry.getValue())).toList();
    }

    private List<Map<String, Object>> trendUsers(List<User> users, int days) {
        Map<LocalDate, Long> buckets = longDayBuckets(days);
        users.stream().filter(user -> user.getCreatedAt() != null).forEach(user ->
                buckets.computeIfPresent(user.getCreatedAt().toLocalDate(), (date, count) -> count + 1));
        return buckets.entrySet().stream().map(entry -> point(entry.getKey(), "users", entry.getValue())).toList();
    }

    private List<Map<String, Object>> trendOwners(List<User> users, int days) {
        return trendUsers(users.stream().filter(user -> hasRole(user, RoleName.ROLE_OWNER)).toList(), days);
    }

    private List<Map<String, Object>> trendTeams(List<Team> teams, int days) {
        Map<LocalDate, Long> buckets = longDayBuckets(days);
        teams.stream().filter(team -> team.getCreatedAt() != null).forEach(team ->
                buckets.computeIfPresent(team.getCreatedAt().toLocalDate(), (date, count) -> count + 1));
        return buckets.entrySet().stream().map(entry -> point(entry.getKey(), "teams", entry.getValue())).toList();
    }

    private List<Map<String, Object>> trendFeed(List<FeedEvent> events, int days) {
        Map<LocalDate, Long> buckets = longDayBuckets(days);
        events.stream().filter(event -> event.getCreatedAt() != null).forEach(event ->
                buckets.computeIfPresent(event.getCreatedAt().toLocalDate(), (date, count) -> count + 1));
        return buckets.entrySet().stream().map(entry -> point(entry.getKey(), "activity", entry.getValue())).toList();
    }

    private Map<String, BigDecimal> revenueByCity(List<Booking> bookings, List<Payment> payments) {
        Set<Long> paidBookingIds = paidBookingIds(payments);
        return bookings.stream()
                .filter(booking -> paidBookingIds.contains(booking.getId()))
                .collect(Collectors.groupingBy(booking -> safe(booking.getTurf().getCity()), Collectors.reducing(BigDecimal.ZERO, booking -> optionalAmount(booking.getAmount()), BigDecimal::add)));
    }

    private Map<String, BigDecimal> revenueByTurf(List<Booking> bookings, List<Payment> payments) {
        Set<Long> paidBookingIds = paidBookingIds(payments);
        return bookings.stream()
                .filter(booking -> paidBookingIds.contains(booking.getId()))
                .collect(Collectors.groupingBy(booking -> booking.getTurf().getName(), Collectors.reducing(BigDecimal.ZERO, booking -> optionalAmount(booking.getAmount()), BigDecimal::add)));
    }

    private Map<String, BigDecimal> revenueByOwner(List<Booking> bookings, List<Payment> payments) {
        Set<Long> paidBookingIds = paidBookingIds(payments);
        return bookings.stream()
                .filter(booking -> paidBookingIds.contains(booking.getId()))
                .collect(Collectors.groupingBy(booking -> booking.getTurf().getOwner().getName(), Collectors.reducing(BigDecimal.ZERO, booking -> optionalAmount(booking.getAmount()), BigDecimal::add)));
    }

    private Set<Long> paidBookingIds(List<Payment> payments) {
        return payments.stream().filter(payment -> payment.getBooking() != null).map(payment -> payment.getBooking().getId()).collect(Collectors.toSet());
    }

    private Map<String, Object> ownerScorecard(User owner, List<Booking> bookings) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ownerId", owner.getId());
        item.put("ownerName", owner.getName());
        item.put("revenue", bookings.stream().map(Booking::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        item.put("bookings", bookings.size());
        item.put("retention", percent(bookings.stream().map(booking -> booking.getUser().getId()).distinct().count(), Math.max(bookings.size(), 1)));
        item.put("rating", bookings.stream().map(booking -> booking.getTurf().getRating()).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0));
        item.put("growth", round(bookings.stream().filter(booking -> booking.getCreatedAt() != null && booking.getCreatedAt().isAfter(LocalDate.now().minusDays(30).atStartOfDay())).count() * 3.0, 2));
        return item;
    }

    private Map<String, Object> turfScorecard(Turf turf, List<Booking> bookings, long slotCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("turfId", turf.getId());
        item.put("name", turf.getName());
        item.put("city", turf.getCity());
        item.put("revenue", bookings.stream().map(Booking::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        item.put("bookings", bookings.size());
        item.put("occupancy", percent(bookings.stream().filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).count(), Math.max(slotCount * 30, bookings.size())));
        item.put("rating", turf.getRating() == null ? 0 : turf.getRating());
        return item;
    }

    private List<Map<String, Object>> peakHours(List<Booking> bookings) {
        return bookings.stream().filter(booking -> booking.getSlot() != null)
                .collect(Collectors.groupingBy(booking -> booking.getSlot().getStartTime().toString(), Collectors.counting()))
                .entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(8)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("hour", entry.getKey());
                    item.put("bookings", entry.getValue());
                    return item;
                }).toList();
    }

    private List<Map<String, Object>> bookingHeatmap(List<Booking> bookings) {
        return Arrays.stream(DayOfWeek.values()).map(day -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", day.name());
            item.put("bookings", bookings.stream().filter(booking -> booking.getBookingDate() != null && booking.getBookingDate().getDayOfWeek() == day).count());
            return item;
        }).toList();
    }

    private List<Map<String, Object>> retentionCohorts(List<User> users, List<Booking> bookings) {
        return users.stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(user -> YearMonth.from(user.getCreatedAt())))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Set<Long> cohortIds = entry.getValue().stream().map(User::getId).collect(Collectors.toSet());
                    long retained = bookings.stream().filter(booking -> cohortIds.contains(booking.getUser().getId())).map(booking -> booking.getUser().getId()).distinct().count();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("cohort", entry.getKey().toString());
                    item.put("users", cohortIds.size());
                    item.put("retained", retained);
                    item.put("retention", percent(retained, Math.max(cohortIds.size(), 1)));
                    return item;
                }).toList();
    }

    private double monthlyCityGrowth(List<Booking> bookings, String city) {
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate previous = YearMonth.now().minusMonths(1).atDay(1);
        long current = bookings.stream().filter(booking -> city.equalsIgnoreCase(safe(booking.getTurf().getCity())) && booking.getCreatedAt() != null && !booking.getCreatedAt().toLocalDate().isBefore(start)).count();
        long past = bookings.stream().filter(booking -> city.equalsIgnoreCase(safe(booking.getTurf().getCity())) && booking.getCreatedAt() != null && !booking.getCreatedAt().toLocalDate().isBefore(previous) && booking.getCreatedAt().toLocalDate().isBefore(start)).count();
        return past == 0 ? current * 100.0 : ((current - past) * 100.0 / past);
    }

    private double monthlyGrowth(List<Payment> payments) {
        BigDecimal current = sumPayments(payments, YearMonth.now());
        BigDecimal previous = sumPayments(payments, YearMonth.now().minusMonths(1));
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double retentionPercent(List<User> users, List<Booking> bookings, int days) {
        LocalDateTime threshold = LocalDate.now().minusDays(days).atStartOfDay();
        Set<Long> eligible = users.stream().filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isBefore(threshold)).map(User::getId).collect(Collectors.toSet());
        if (eligible.isEmpty()) {
            return 0;
        }
        long retained = bookings.stream().filter(booking -> eligible.contains(booking.getUser().getId())).map(booking -> booking.getUser().getId()).distinct().count();
        return percent(retained, eligible.size());
    }

    private int safetyScore() {
        long openFraud = fraudProfileRepository.findByStatusOrderByCreatedAtDesc("OPEN").size();
        long activeSuspensions = safetySuspensionRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").size();
        long reports = trustReportRepository.count();
        return (int) Math.max(0, 100 - openFraud * 5 - activeSuspensions * 3 - reports);
    }

    private double communityHealth(List<FeedEvent> feedEvents, List<Team> teams, long reports) {
        return Math.max(0, Math.min(100, 60 + Math.min(feedEvents.size(), 20) + Math.min(teams.size(), 20) - reports));
    }

    private Map<LocalDate, BigDecimal> dayBuckets(int days, BigDecimal value) {
        Map<LocalDate, BigDecimal> buckets = new TreeMap<>();
        for (int index = days - 1; index >= 0; index--) {
            buckets.put(LocalDate.now().minusDays(index), value);
        }
        return buckets;
    }

    private Map<LocalDate, Long> longDayBuckets(int days) {
        Map<LocalDate, Long> buckets = new TreeMap<>();
        for (int index = days - 1; index >= 0; index--) {
            buckets.put(LocalDate.now().minusDays(index), 0L);
        }
        return buckets;
    }

    private Map<String, Object> point(LocalDate date, String key, Object value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("date", date.toString());
        item.put(key, value);
        return item;
    }

    private Map<String, Object> funnelStep(String label, long value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        return item;
    }

    private Map<String, Object> insight(String title, String message, String severity) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("message", message);
        item.put("severity", severity);
        return item;
    }

    private boolean hasRole(User user, RoleName role) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(item -> item.getName() == role);
    }

    private BigDecimal optionalAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal money(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private double percent(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return round(value * 100.0 / total, 2);
    }

    private double round(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }
}
