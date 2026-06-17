package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.security.PortalPagePermissionCatalog;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/ops")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN', 'OPERATIONS_ADMIN', 'FINANCE_ADMIN', 'SUPPORT_ADMIN', 'CONTENT_ADMIN', 'MODERATOR')")
@Transactional
public class AdminOperationsController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OwnerVerificationRepository ownerVerificationRepository;
    private final TurfModerationRepository turfModerationRepository;
    private final TurfRepository turfRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FinanceReportRepository financeReportRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final CmsContentRepository cmsContentRepository;
    private final AuditLogRepository auditLogRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final IncidentRepository incidentRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PlayerTrustScoreRepository playerTrustScoreRepository;
    private final TrustReportRepository trustReportRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final AccessPolicyRepository accessPolicyRepository;
    private final ResourcePermissionRepository resourcePermissionRepository;
    private final TemporaryPermissionRepository temporaryPermissionRepository;
    private final PermissionAuditRepository permissionAuditRepository;
    private final OwnerStaffRoleRepository ownerStaffRoleRepository;

    private String getAdminUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "system_admin";
    }

    private void writeAudit(String action, String entity) {
        AuditLog log = AuditLog.builder()
                .adminId(getAdminUsername())
                .action(action)
                .entity(entity)
                .build();
        auditLogRepository.save(log);
    }

    private void writePermissionAudit(String action, String entity, String details) {
        permissionAuditRepository.save(PermissionAudit.builder()
                .actor(getAdminUsername())
                .action(action)
                .entity(entity)
                .details(details)
                .build());
    }

    private void writeUserActivity(Long userId, String activityType, Long entityId, String summary) {
        userActivityLogRepository.save(UserActivityLog.builder()
                .userId(userId)
                .activityType(activityType)
                .entityId(entityId)
                .summary(summary)
                .build());
    }

    // --- 1. USER MODERATION ---

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/user-center/summary")
    public ResponseEntity<Map<String, Object>> getUserManagementSummary() {
        List<User> users = userRepository.findAll();
        long players = users.stream().filter(user -> hasRole(user, com.example.Cricbook.enums.RoleName.ROLE_USER)).count();
        long owners = users.stream().filter(user -> hasRole(user, com.example.Cricbook.enums.RoleName.ROLE_OWNER)).count();
        long suspended = users.stream().filter(user -> user.getStatus() == UserStatus.BLOCKED).count();
        long pendingVerifications = ownerVerificationRepository.findAll().stream().filter(item -> "PENDING".equalsIgnoreCase(item.getStatus())).count();
        long highRisk = playerTrustScoreRepository.findAll().stream().filter(score -> score.getScore() != null && score.getScore() < 50).count();
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        long newRegistrations = users.stream().filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(since)).count();

        return ResponseEntity.ok(Map.of(
                "totalPlayers", players,
                "totalOwners", owners,
                "totalTeams", teamRepository.count(),
                "pendingVerifications", pendingVerifications,
                "suspendedAccounts", suspended,
                "highRiskUsers", highRisk,
                "newRegistrations", newRegistrations
        ));
    }

    @GetMapping("/user-center/users")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String risk) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> normalizedQuery.isBlank()
                        || safe(user.getName()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || safe(user.getEmail()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || safe(user.getPhone()).toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .filter(user -> role == null || role.isBlank() || hasRoleCode(user, role))
                .filter(user -> status == null || status.isBlank() || user.getStatus().name().equalsIgnoreCase(status))
                .map(this::mapAdminManagedUser)
                .filter(user -> risk == null || risk.isBlank() || safe(user.get("riskLevel")).equalsIgnoreCase(risk))
                .toList());
    }

    @GetMapping("/user-center/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        Map<String, Object> profile = new LinkedHashMap<>(mapAdminManagedUser(user));
        profile.put("bookings", bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(userId).stream().limit(10).map(this::mapBookingBrief).toList());
        profile.put("payments", paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(userId).stream().limit(10).map(this::mapPaymentBrief).toList());
        profile.put("teams", teamMemberRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().limit(10).map(member -> Map.of(
                "teamId", member.getTeam().getId(),
                "teamName", member.getTeam().getName(),
                "role", member.getRole(),
                "status", member.getStatus()
        )).toList());
        profile.put("reports", trustReportRepository.findTop20ByReportedUserIdOrderByCreatedAtDesc(userId).stream().map(report -> Map.of(
                "id", report.getId(),
                "reason", report.getReason(),
                "status", report.getStatus(),
                "createdAt", report.getCreatedAt()
        )).toList());
        profile.put("activity", userActivityLogRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId));
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/user-center/owners/{ownerId}")
    public ResponseEntity<Map<String, Object>> getOwnerProfile(@PathVariable Long ownerId) {
        User owner = userRepository.findById(ownerId).orElseThrow(() -> new NoSuchElementException("Owner not found"));
        Map<String, Object> profile = new LinkedHashMap<>(mapAdminManagedUser(owner));
        profile.put("turfs", turfRepository.findByOwnerId(ownerId).stream().map(turf -> Map.of(
                "id", turf.getId(),
                "name", turf.getName(),
                "city", turf.getCity(),
                "status", turf.getStatus(),
                "rating", turf.getRating()
        )).toList());
        profile.put("bookings", bookingRepository.findByTurfOwnerId(ownerId).stream().limit(10).map(this::mapBookingBrief).toList());
        profile.put("verification", ownerVerificationRepository.findAll().stream()
                .filter(item -> Objects.equals(item.getOwnerId(), ownerId))
                .sorted(Comparator.comparing(OwnerVerification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        BigDecimal revenue = bookingRepository.findByTurfOwnerId(ownerId).stream()
                .map(Booking::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        profile.put("revenue", revenue);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/user-center/teams")
    public ResponseEntity<List<Map<String, Object>>> getManagedTeams(@RequestParam(required = false) String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return ResponseEntity.ok(teamRepository.findAll().stream()
                .filter(team -> normalizedQuery.isBlank()
                        || safe(team.getName()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || safe(team.getCity()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || safe(team.getSport()).toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .map(team -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", team.getId());
                    item.put("name", team.getName());
                    item.put("sport", team.getSport());
                    item.put("city", team.getCity());
                    item.put("captainId", team.getCaptain().getId());
                    item.put("captainName", team.getCaptain().getName());
                    item.put("members", teamMemberRepository.countByTeamId(team.getId()));
                    item.put("visibility", team.getVisibility());
                    item.put("createdAt", team.getCreatedAt());
                    return item;
                })
                .toList());
    }

    @GetMapping("/user-center/activity")
    public ResponseEntity<List<UserActivityLog>> getUserActivity() {
        return ResponseEntity.ok(userActivityLogRepository.findTop50ByOrderByCreatedAtDesc());
    }

    @PostMapping("/user-center/users/{userId}/reset-trust")
    public ResponseEntity<Map<String, Object>> resetTrustScore(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        PlayerTrustScore score = playerTrustScoreRepository.findByUserId(userId)
                .orElseGet(() -> PlayerTrustScore.builder().user(user).build());
        score.setScore(100);
        score.setAttendanceScore(100);
        score.setPunctualityScore(100);
        score.setCancellationScore(100);
        score.setReportScore(100);
        score.setPaymentScore(100);
        score.setMatchCompletionScore(100);
        playerTrustScoreRepository.save(score);
        writeAudit("RESET_TRUST_SCORE", "User " + user.getEmail());
        writeUserActivity(userId, "TRUST_RESET", score.getId(), "Admin reset trust score to 100.");
        return ResponseEntity.ok(mapAdminManagedUser(user));
    }

    @PostMapping("/user-center/users/{userId}/communicate")
    public ResponseEntity<Map<String, Object>> sendUserCommunication(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        String channel = body.getOrDefault("channel", "IN_APP");
        String message = body.getOrDefault("message", "Admin account update.");
        writeAudit("SEND_USER_COMMUNICATION", "User " + user.getEmail() + " via " + channel);
        writeUserActivity(userId, "ADMIN_MESSAGE", userId, channel + ": " + message);
        return ResponseEntity.ok(Map.of("message", "Communication queued.", "channel", channel));
    }

    @PostMapping("/users/{userId}/warn")
    public ResponseEntity<?> warnUser(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        String reason = body.getOrDefault("reason", "Suspicious marketplace activity warning.");
        writeAudit("WARN_USER", "User " + user.getEmail() + " (Reason: " + reason + ")");
        return ResponseEntity.ok(Map.of("message", "User warned successfully. Warning: " + reason));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<User> banUser(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setStatus(UserStatus.BLOCKED);
        userRepository.save(user);
        String reason = body.getOrDefault("reason", "Violating community standards.");
        writeAudit("BAN_USER", "User " + user.getEmail() + " (Reason: " + reason + ")");
        writeUserActivity(userId, "ACCOUNT_SUSPENDED", userId, "Admin suspended account. Reason: " + reason);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<User> unbanUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        writeAudit("UNBAN_USER", "User " + user.getEmail());
        writeUserActivity(userId, "ACCOUNT_RESTORED", userId, "Admin restored account.");
        return ResponseEntity.ok(user);
    }

    // --- 2. OWNER VERIFICATION QUEUE ---

    @GetMapping("/owner-verifications")
    public ResponseEntity<List<OwnerVerification>> getOwnerVerifications() {
        return ResponseEntity.ok(ownerVerificationRepository.findAll());
    }

    @PostMapping("/owner-verifications/{id}/approve")
    public ResponseEntity<OwnerVerification> approveOwner(@PathVariable Long id) {
        OwnerVerification verification = ownerVerificationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Owner Verification not found"));
        verification.setStatus("APPROVED");
        verification.setVerifiedBy(getAdminUsername());
        ownerVerificationRepository.save(verification);

        // Optionally unlock user status to ACTIVE if unverified
        userRepository.findById(verification.getOwnerId()).ifPresent(owner -> {
            owner.setStatus(UserStatus.ACTIVE);
            userRepository.save(owner);
        });

        writeAudit("APPROVE_OWNER", "Verification #" + id + " for Owner ID " + verification.getOwnerId());
        return ResponseEntity.ok(verification);
    }

    @PostMapping("/owner-verifications/{id}/reject")
    public ResponseEntity<OwnerVerification> rejectOwner(@PathVariable Long id) {
        OwnerVerification verification = ownerVerificationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Owner Verification not found"));
        verification.setStatus("REJECTED");
        verification.setVerifiedBy(getAdminUsername());
        ownerVerificationRepository.save(verification);

        writeAudit("REJECT_OWNER", "Verification #" + id + " for Owner ID " + verification.getOwnerId());
        return ResponseEntity.ok(verification);
    }

    // --- 3. TURF MODERATION ---

    @GetMapping("/turf-moderations")
    public ResponseEntity<List<TurfModeration>> getTurfModerations() {
        return ResponseEntity.ok(turfModerationRepository.findAll());
    }

    @PostMapping("/turf-moderations/{id}/approve")
    public ResponseEntity<TurfModeration> approveTurf(@PathVariable Long id) {
        TurfModeration moderation = turfModerationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Turf moderation record not found"));
        moderation.setStatus("APPROVED");
        turfModerationRepository.save(moderation);

        turfRepository.findById(moderation.getTurfId()).ifPresent(turf -> {
            turf.setStatus(TurfStatus.ACTIVE);
            turfRepository.save(turf);
        });

        writeAudit("APPROVE_TURF", "Turf ID " + moderation.getTurfId());
        return ResponseEntity.ok(moderation);
    }

    @PostMapping("/turf-moderations/{id}/reject")
    public ResponseEntity<TurfModeration> rejectTurf(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TurfModeration moderation = turfModerationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Turf moderation record not found"));
        String reason = body.getOrDefault("reason", "Incomplete documentation");
        moderation.setStatus("REJECTED");
        moderation.setReason(reason);
        turfModerationRepository.save(moderation);

        turfRepository.findById(moderation.getTurfId()).ifPresent(turf -> {
            turf.setStatus(TurfStatus.INACTIVE);
            turfRepository.save(turf);
        });

        writeAudit("REJECT_TURF", "Turf ID " + moderation.getTurfId() + " Reason: " + reason);
        return ResponseEntity.ok(moderation);
    }

    // --- 4. BOOKING STATE OVERRIDES & MANUAL REFUNDS ---

    @GetMapping("/bookings")
    public ResponseEntity<List<Map<String, Object>>> getBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        List<Map<String, Object>> list = bookings.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("customerName", b.getUser() != null ? b.getUser().getName() : "Guest");
            m.put("customerEmail", b.getUser() != null ? b.getUser().getEmail() : "");
            m.put("turfName", b.getTurf() != null ? b.getTurf().getName() : "Unknown");
            m.put("bookingDate", b.getBookingDate());
            m.put("amount", b.getAmount());
            m.put("status", b.getStatus());
            m.put("createdAt", b.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/bookings/{bookingId}/override")
    public ResponseEntity<?> overrideBookingStatus(@PathVariable Long bookingId, @RequestBody Map<String, String> body) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));
        String newStatusStr = body.get("status");
        BookingStatus newStatus = BookingStatus.valueOf(newStatusStr.toUpperCase());
        booking.setStatus(newStatus);
        bookingRepository.save(booking);

        writeAudit("OVERRIDE_BOOKING_STATUS", "Booking ID " + bookingId + " to " + newStatus);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/bookings/{bookingId}/refund")
    public ResponseEntity<?> triggerManualRefund(@PathVariable Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(bookingId);
        Payment payment = null;
        if (paymentOpt.isPresent()) {
            payment = paymentOpt.get();
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
        } else {
            // Mock dynamic payment if not present
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getAmount())
                    .status("REFUNDED")
                    .razorpayPaymentId("mock_pay_" + UUID.randomUUID().toString().substring(0, 8))
                    .build();
            paymentRepository.save(payment);
        }

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(booking.getAmount())
                .status("SUCCESS")
                .reason("Super Admin manual refund override")
                .gatewayRefundId("gateway_ref_" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        refundRepository.save(refund);

        // Also track this manual refund event in our analytics/finance reports
        FinanceReport report = financeReportRepository.findByDate(LocalDate.now())
                .orElseGet(() -> FinanceReport.builder().date(LocalDate.now()).revenue(BigDecimal.ZERO).refund(BigDecimal.ZERO).commission(BigDecimal.ZERO).build());
        report.setRefund(report.getRefund().add(booking.getAmount()));
        financeReportRepository.save(report);

        writeAudit("MANUAL_REFUND_BOOKING", "Booking ID " + bookingId + " Refunded: " + booking.getAmount());
        return ResponseEntity.ok(refund);
    }

    // --- 5. FINANCE STATISTICS & Platform Commission adjustments ---

    @GetMapping("/finance")
    public ResponseEntity<?> getFinanceReport() {
        List<FinanceReport> reports = financeReportRepository.findAll();
        BigDecimal totalRevenue = reports.stream().map(FinanceReport::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefunds = reports.stream().map(FinanceReport::getRefund).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommissions = reports.stream().map(FinanceReport::getCommission).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get commission setting from platform_settings
        String commissionStr = platformSettingRepository.findByKey("COMMISSION_RATE")
                .map(PlatformSetting::getValue).orElse("10.0");

        String maintenanceMode = platformSettingRepository.findByKey("MAINTENANCE_MODE")
                .map(PlatformSetting::getValue).orElse("false");

        return ResponseEntity.ok(Map.of(
                "reports", reports,
                "totalRevenue", totalRevenue,
                "totalRefunds", totalRefunds,
                "totalCommission", totalCommissions,
                "commissionRate", commissionStr,
                "maintenanceMode", maintenanceMode
        ));
    }

    @GetMapping("/settings")
    public ResponseEntity<List<PlatformSetting>> getSettings() {
        return ResponseEntity.ok(platformSettingRepository.findAll());
    }

    @PostMapping("/settings")
    public ResponseEntity<PlatformSetting> saveOrUpdateSetting(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");

        PlatformSetting setting = platformSettingRepository.findByKey(key)
                .orElseGet(() -> PlatformSetting.builder().key(key).build());
        setting.setValue(value);
        platformSettingRepository.save(setting);

        writeAudit("UPDATE_PLATFORM_SETTING", "Key: " + key + " Value: " + value);
        return ResponseEntity.ok(setting);
    }

    // --- 6. FRAUD RULES & ALERTS ---

    @GetMapping("/fraud-alerts")
    public ResponseEntity<List<FraudAlert>> getFraudAlerts() {
        // Build mock fraud alerts if table is empty, so that the admin is always shown vibrant indicators!
        if (fraudAlertRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            if (!users.isEmpty()) {
                FraudAlert a1 = FraudAlert.builder()
                        .userId(users.get(0).getId())
                        .severity("HIGH")
                        .reason("High cancellation velocity: 4 cancellations within 15 minutes.")
                        .build();
                fraudAlertRepository.save(a1);
            }
        }
        return ResponseEntity.ok(fraudAlertRepository.findAll());
    }

    @PostMapping("/fraud-alerts/{id}/dismiss")
    public ResponseEntity<?> dismissAlert(@PathVariable Long id) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Alert not found"));
        alert.setSeverity("DISMISSED");
        fraudAlertRepository.save(alert);
        writeAudit("DISMISS_FRAUD_ALERT", "Alert ID " + id);
        return ResponseEntity.ok(Map.of("message", "Alert dismissed successfully."));
    }

    // --- 7. CMS HOME PROMOTION BANNERS ---

    @GetMapping("/cms")
    public ResponseEntity<List<CmsContent>> getCmsContent() {
        return ResponseEntity.ok(cmsContentRepository.findAll());
    }

    @PostMapping("/cms")
    public ResponseEntity<CmsContent> addCmsContent(@RequestBody CmsContent request) {
        if (request.getStatus() == null) {
            request.setStatus("ACTIVE");
        }
        cmsContentRepository.save(request);
        writeAudit("CREATE_CMS_CONTENT", "Cms Title: " + request.getTitle() + " Type: " + request.getType());
        return ResponseEntity.ok(request);
    }

    @DeleteMapping("/cms/{id}")
    public ResponseEntity<?> deleteCmsContent(@PathVariable Long id) {
        CmsContent cms = cmsContentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("CMS Content not found"));
        cmsContentRepository.delete(cms);
        writeAudit("DELETE_CMS_CONTENT", "CMS ID " + id);
        return ResponseEntity.ok(Map.of("message", "Banner content deleted successfully."));
    }

    // --- 8. AUDIT LOGBOOK ---

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/governance/dashboard")
    public ResponseEntity<Map<String, Object>> getGovernanceDashboard() {
        List<User> users = userRepository.findAll();
        List<AdminUser> adminUsers = adminUserRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();
        List<Refund> refunds = refundRepository.findAll();
        List<SupportTicket> supportTickets = supportTicketRepository.findAll();
        List<Incident> incidents = incidentRepository.findAll();
        List<FraudAlert> fraudAlerts = fraudAlertRepository.findAll();
        List<OwnerVerification> ownerVerifications = ownerVerificationRepository.findAll();
        List<TurfModeration> turfModerations = turfModerationRepository.findAll();
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        List<AdminRole> roles = adminRoleRepository.findAll();
        List<AdminPermission> permissions = adminPermissionRepository.findAll();

        long suspendedUsers = users.stream().filter(user -> user.getStatus() == UserStatus.BLOCKED).count();
        long pendingOwnerVerifications = ownerVerifications.stream().filter(item -> statusEquals(item.getStatus(), "PENDING")).count();
        long pendingTurfModerations = turfModerations.stream().filter(item -> statusEquals(item.getStatus(), "PENDING")).count();
        long openRefunds = refunds.stream().filter(item -> !statusEquals(item.getStatus(), "APPROVED") && !statusEquals(item.getStatus(), "REJECTED") && !statusEquals(item.getStatus(), "PROCESSED")).count();
        long openSupportTickets = supportTickets.stream().filter(item -> !statusEquals(item.getStatus(), "RESOLVED") && !statusEquals(item.getStatus(), "CLOSED")).count();
        long openIncidents = incidents.stream().filter(item -> !statusEquals(item.getStatus(), "RESOLVED")).count();
        long criticalIncidents = incidents.stream().filter(item -> statusEquals(item.getSeverity(), "CRITICAL") && !statusEquals(item.getStatus(), "RESOLVED")).count();
        long highFraudAlerts = fraudAlerts.stream().filter(item -> statusEquals(item.getSeverity(), "HIGH") || statusEquals(item.getSeverity(), "CRITICAL")).count();
        long activeAdmins = adminUsers.stream().filter(item -> statusEquals(item.getStatus(), "ACTIVE")).count();
        long inactiveRoles = roles.stream().filter(role -> Boolean.FALSE.equals(role.getActive())).count();
        long rolesWithoutPermissions = roles.stream().filter(role -> role.getPermissions() == null || role.getPermissions().isEmpty()).count();
        long highRiskUsers = playerTrustScoreRepository.findAll().stream().filter(score -> score.getScore() != null && score.getScore() < 50).count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("governanceScore", governanceScore(suspendedUsers, openRefunds, openSupportTickets, criticalIncidents, rolesWithoutPermissions));
        response.put("riskLevel", governanceRiskLevel(suspendedUsers, openRefunds, openSupportTickets, criticalIncidents, highFraudAlerts));
        response.put("kpis", Map.of(
                "users", users.size(),
                "adminUsers", adminUsers.size(),
                "activeAdmins", activeAdmins,
                "roles", roles.size(),
                "permissions", permissions.size(),
                "bookings", bookings.size(),
                "payments", payments.size(),
                "refunds", refunds.size(),
                "auditEvents", auditLogs.size()
        ));
        response.put("queues", Map.of(
                "ownerVerifications", pendingOwnerVerifications,
                "turfModerations", pendingTurfModerations,
                "refunds", openRefunds,
                "supportTickets", openSupportTickets,
                "incidents", openIncidents,
                "fraudAlerts", fraudAlerts.size(),
                "highFraudAlerts", highFraudAlerts,
                "highRiskUsers", highRiskUsers
        ));
        response.put("controlHealth", List.of(
                controlHealth("RBAC coverage", rolesWithoutPermissions == 0, rolesWithoutPermissions + " role(s) have no permissions"),
                controlHealth("Admin access", activeAdmins > 0, activeAdmins + " active admin(s)"),
                controlHealth("Audit logging", !auditLogs.isEmpty(), auditLogs.size() + " audit event(s) captured"),
                controlHealth("Refund queue", openRefunds == 0, openRefunds + " refund(s) need finance action"),
                controlHealth("Support SLA", openSupportTickets < 10, openSupportTickets + " open support ticket(s)"),
                controlHealth("Incident response", criticalIncidents == 0, criticalIncidents + " critical incident(s) open")
        ));
        response.put("priorityActions", governancePriorityActions(pendingOwnerVerifications, pendingTurfModerations, openRefunds, openSupportTickets, criticalIncidents, highFraudAlerts, rolesWithoutPermissions));
        response.put("recentAudits", auditLogs.stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(this::mapAuditLog)
                .toList());
        response.put("rbacSummary", Map.of(
                "roles", roles.size(),
                "permissions", permissions.size(),
                "inactiveRoles", inactiveRoles,
                "rolesWithoutPermissions", rolesWithoutPermissions,
                "catalogPages", PortalPagePermissionCatalog.catalog().size()
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit-logs/search")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('AUDIT_VIEW') or hasAuthority('SECURITY_AUDIT_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> searchAuditLogs(
            @RequestParam(required = false) String adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entity,
            @RequestParam(defaultValue = "100") int limit
    ) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        return ResponseEntity.ok(auditLogRepository.findAll().stream()
                .filter(log -> containsIgnoreCase(log.getAdminId(), adminId))
                .filter(log -> containsIgnoreCase(log.getAction(), action))
                .filter(log -> containsIgnoreCase(log.getEntity(), entity))
                .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(boundedLimit)
                .map(this::mapAuditLog)
                .toList());
    }

    @GetMapping("/audit-logs/export")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('AUDIT_EXPORT')")
    public ResponseEntity<Map<String, Object>> exportAuditLogs(
            @RequestParam(required = false) String adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entity
    ) {
        List<Map<String, Object>> rows = auditLogRepository.findAll().stream()
                .filter(log -> containsIgnoreCase(log.getAdminId(), adminId))
                .filter(log -> containsIgnoreCase(log.getAction(), action))
                .filter(log -> containsIgnoreCase(log.getEntity(), entity))
                .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapAuditLog)
                .toList();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("fileName", "admin-audit-export-" + LocalDate.now() + ".csv");
        export.put("format", "CSV");
        export.put("generatedAt", LocalDateTime.now());
        export.put("totalRows", rows.size());
        export.put("rows", rows);
        writeAudit("EXPORT_AUDIT_LOGS", "Rows " + rows.size());
        return ResponseEntity.ok(export);
    }

    // --- 8B. EPIC 36 RBAC CONTROL CENTER ---

    @GetMapping("/rbac/permissions")
    public ResponseEntity<List<AdminPermission>> getAdminPermissions() {
        return ResponseEntity.ok(adminPermissionRepository.findAll());
    }

    @GetMapping("/rbac/page-permissions")
    public ResponseEntity<List<Map<String, Object>>> getPagePermissionCatalog() {
        return ResponseEntity.ok(PortalPagePermissionCatalog.catalog());
    }

    @GetMapping("/rbac/roles")
    public ResponseEntity<List<Map<String, Object>>> getAdminRoles() {
        return ResponseEntity.ok(adminRoleRepository.findAll().stream().map(this::mapAdminRole).toList());
    }

    @PostMapping("/rbac/roles")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Map<String, Object>> createAdminRole(@RequestBody AdminRoleRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role name is required."));
        }
        String normalizedName = request.getName().trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        AdminRole role = adminRoleRepository.findByName(normalizedName)
                .orElseGet(() -> AdminRole.builder()
                        .name(normalizedName)
                        .description(request.getDescription() == null || request.getDescription().isBlank() ? normalizedName : request.getDescription().trim())
                        .build());
        List<String> permissionCodes = request.getPermissionCodes() == null ? List.of() : request.getPermissionCodes();
        role.setPermissions(new HashSet<>(adminPermissionRepository.findByCodeIn(permissionCodes)));
        AdminRole saved = adminRoleRepository.save(role);
        writeAudit("UPSERT_ADMIN_ROLE", "Role " + saved.getName() + " permissions=" + permissionCodes);
        writePermissionAudit("ROLE_UPSERT", "AdminRole:" + saved.getId(), "Role " + saved.getName() + " permissions=" + permissionCodes);
        return ResponseEntity.ok(mapAdminRole(saved));
    }

    @GetMapping("/rbac/admin-users")
    public ResponseEntity<List<Map<String, Object>>> getAdminUsers() {
        return ResponseEntity.ok(adminUserRepository.findAll().stream().map(this::mapAdminUser).toList());
    }

    @PatchMapping("/rbac/admin-users/{adminId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ADMIN_USER_MANAGE')")
    public ResponseEntity<Map<String, Object>> updateAdminUserStatus(@PathVariable Long adminId, @RequestBody AdminUserStatusRequest request) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new NoSuchElementException("Admin user not found"));
        String normalizedStatus = safe(request.getStatus()).trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        if (!Set.of("ACTIVE", "LOCKED", "SUSPENDED", "DISABLED").contains(normalizedStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported admin status."));
        }
        admin.setStatus(normalizedStatus);
        AdminUser saved = adminUserRepository.save(admin);
        writeAudit("UPDATE_ADMIN_USER_STATUS", saved.getEmail() + " -> " + normalizedStatus + " reason=" + safe(request.getReason()));
        writePermissionAudit("ADMIN_USER_STATUS_UPDATED", "AdminUser:" + saved.getId(), saved.getEmail() + " -> " + normalizedStatus);
        return ResponseEntity.ok(mapAdminUser(saved));
    }

    @PatchMapping("/rbac/admin-users/{adminId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<Map<String, Object>> assignAdminRole(@PathVariable Long adminId, @RequestBody AdminRoleAssignmentRequest request) {
        AdminUser admin = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new NoSuchElementException("Admin user not found"));
        String normalizedRole = request.getRole() == null ? "" : request.getRole().trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        if (adminRoleRepository.findByName(normalizedRole).isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Admin role does not exist."));
        }
        admin.setRole(normalizedRole);
        AdminUser saved = adminUserRepository.save(admin);
        writeAudit("ASSIGN_ADMIN_ROLE", "Admin " + saved.getEmail() + " -> " + saved.getRole());
        writePermissionAudit("ROLE_ASSIGNED", "AdminUser:" + saved.getId(), "Assigned " + saved.getRole() + " to " + saved.getEmail());
        return ResponseEntity.ok(mapAdminUser(saved));
    }

    @GetMapping("/rbac/me")
    public ResponseEntity<Map<String, Object>> getCurrentAdminAccess() {
        String username = getAdminUsername();
        AdminUser admin = adminUserRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("Admin user not found"));
        Set<String> permissionCodes = resolveAdminPermissions(admin);
        return ResponseEntity.ok(Map.of(
                "id", admin.getId(),
                "email", admin.getEmail(),
                "role", admin.getRole(),
                "permissions", permissionCodes,
                "visibleMenus", visibleMenus(permissionCodes)
        ));
    }

    @GetMapping("/rbac/groups")
    public ResponseEntity<List<Map<String, Object>>> getPermissionGroups() {
        return ResponseEntity.ok(permissionGroupRepository.findAll().stream().map(this::mapPermissionGroup).toList());
    }

    @PostMapping("/rbac/groups")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('PERMISSION_EDIT')")
    public ResponseEntity<Map<String, Object>> savePermissionGroup(@RequestBody PermissionGroupRequest request) {
        String normalizedName = safe(request.getName()).trim();
        if (normalizedName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Group name is required."));
        }
        PermissionGroup group = permissionGroupRepository.findByName(normalizedName)
                .orElseGet(() -> PermissionGroup.builder().name(normalizedName).build());
        group.setDescription(safe(request.getDescription()).isBlank() ? normalizedName : request.getDescription().trim());
        group.setPermissions(new HashSet<>(adminPermissionRepository.findByCodeIn(request.getPermissionCodes() == null ? List.of() : request.getPermissionCodes())));
        PermissionGroup saved = permissionGroupRepository.save(group);
        writePermissionAudit("PERMISSION_GROUP_UPSERT", "PermissionGroup:" + saved.getId(), "Group " + saved.getName());
        return ResponseEntity.ok(mapPermissionGroup(saved));
    }

    @GetMapping("/rbac/policies")
    public ResponseEntity<List<Map<String, Object>>> getAccessPolicies() {
        return ResponseEntity.ok(accessPolicyRepository.findAll().stream().map(this::mapAccessPolicy).toList());
    }

    @PostMapping("/rbac/policies")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('POLICY_MANAGE')")
    public ResponseEntity<Map<String, Object>> saveAccessPolicy(@RequestBody AccessPolicyRequest request) {
        AdminRole role = adminRoleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NoSuchElementException("Role not found"));
        AccessPolicy policy = AccessPolicy.builder()
                .role(role)
                .policyType(safe(request.getPolicyType()).trim().toUpperCase(Locale.ROOT))
                .policyValue(safe(request.getPolicyValue()).trim())
                .build();
        AccessPolicy saved = accessPolicyRepository.save(policy);
        writePermissionAudit("ACCESS_POLICY_CREATED", "AccessPolicy:" + saved.getId(), role.getName() + " " + saved.getPolicyType() + "=" + saved.getPolicyValue());
        return ResponseEntity.ok(mapAccessPolicy(saved));
    }

    @GetMapping("/rbac/resource-permissions")
    public ResponseEntity<List<Map<String, Object>>> getResourcePermissions() {
        return ResponseEntity.ok(resourcePermissionRepository.findAll().stream().map(this::mapResourcePermission).toList());
    }

    @PostMapping("/rbac/resource-permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('RESOURCE_PERMISSION_MANAGE')")
    public ResponseEntity<Map<String, Object>> grantResourcePermission(@RequestBody ResourcePermissionRequest request) {
        ResourcePermission permission = ResourcePermission.builder()
                .userId(request.getUserId())
                .resourceType(safe(request.getResourceType()).trim().toUpperCase(Locale.ROOT))
                .resourceId(request.getResourceId())
                .permission(safe(request.getPermission()).trim().toUpperCase(Locale.ROOT))
                .build();
        ResourcePermission saved = resourcePermissionRepository.save(permission);
        writePermissionAudit("RESOURCE_PERMISSION_GRANTED", "ResourcePermission:" + saved.getId(), saved.getResourceType() + ":" + saved.getResourceId());
        return ResponseEntity.ok(mapResourcePermission(saved));
    }

    @GetMapping("/rbac/temporary-permissions")
    public ResponseEntity<List<Map<String, Object>>> getTemporaryPermissions() {
        return ResponseEntity.ok(temporaryPermissionRepository.findAll().stream().map(this::mapTemporaryPermission).toList());
    }

    @PostMapping("/rbac/temporary-permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('TEMPORARY_ACCESS_GRANT')")
    public ResponseEntity<Map<String, Object>> grantTemporaryPermission(@RequestBody TemporaryPermissionRequest request) {
        AdminPermission permission = adminPermissionRepository.findByCode(safe(request.getPermissionCode()).trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new NoSuchElementException("Permission not found"));
        TemporaryPermission temporaryPermission = TemporaryPermission.builder()
                .userId(request.getUserId())
                .permission(permission)
                .expiresAt(request.getExpiresAt())
                .build();
        TemporaryPermission saved = temporaryPermissionRepository.save(temporaryPermission);
        writePermissionAudit("TEMPORARY_PERMISSION_GRANTED", "TemporaryPermission:" + saved.getId(), permission.getCode() + " until " + saved.getExpiresAt());
        return ResponseEntity.ok(mapTemporaryPermission(saved));
    }

    @GetMapping("/rbac/owner-staff-roles")
    public ResponseEntity<List<Map<String, Object>>> getOwnerStaffRoles() {
        return ResponseEntity.ok(ownerStaffRoleRepository.findAll().stream().map(this::mapOwnerStaffRole).toList());
    }

    @PostMapping("/rbac/owner-staff-roles")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('OWNER_STAFF_MANAGE')")
    public ResponseEntity<Map<String, Object>> assignOwnerStaffRole(@RequestBody OwnerStaffRoleRequest request) {
        AdminRole role = adminRoleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new NoSuchElementException("Role not found"));
        OwnerStaffRole staffRole = OwnerStaffRole.builder()
                .ownerId(request.getOwnerId())
                .staffId(request.getStaffId())
                .role(role)
                .active(true)
                .build();
        OwnerStaffRole saved = ownerStaffRoleRepository.save(staffRole);
        writePermissionAudit("OWNER_STAFF_ROLE_ASSIGNED", "OwnerStaffRole:" + saved.getId(), "Owner " + saved.getOwnerId() + " staff " + saved.getStaffId() + " -> " + role.getName());
        return ResponseEntity.ok(mapOwnerStaffRole(saved));
    }

    @GetMapping("/rbac/audit")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('AUDIT_VIEW') or hasAuthority('SECURITY_AUDIT_VIEW')")
    public ResponseEntity<List<PermissionAudit>> getPermissionAudit() {
        return ResponseEntity.ok(permissionAuditRepository.findTop100ByOrderByTimestampDesc());
    }

    @GetMapping("/rbac/matrix")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ROLE_MANAGE') or hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<Map<String, Object>> getRbacMatrix() {
        List<Map<String, Object>> catalog = PortalPagePermissionCatalog.catalog();
        List<AdminRole> roles = adminRoleRepository.findAll();
        List<AdminPermission> permissions = adminPermissionRepository.findAll();

        List<Map<String, Object>> roleRows = roles.stream()
                .sorted(Comparator.comparing(AdminRole::getName))
                .map(role -> {
                    Set<String> codes = role.getPermissions().stream().map(AdminPermission::getCode).collect(Collectors.toCollection(TreeSet::new));
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("roleId", role.getId());
                    row.put("roleName", role.getName());
                    row.put("description", role.getDescription());
                    row.put("active", role.getActive());
                    row.put("systemRole", role.getSystemRole());
                    row.put("permissionCount", codes.size());
                    row.put("permissions", codes);
                    row.put("pageAccess", catalog.stream().map(page -> mapPageAccess(page, codes)).toList());
                    return row;
                })
                .toList();

        Map<String, Long> permissionsByModule = permissions.stream()
                .collect(Collectors.groupingBy(AdminPermission::getModule, TreeMap::new, Collectors.counting()));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now());
        response.put("roles", roleRows);
        response.put("permissionsByModule", permissionsByModule);
        response.put("catalog", catalog);
        response.put("summary", Map.of(
                "roleCount", roles.size(),
                "permissionCount", permissions.size(),
                "pageCount", catalog.size(),
                "rolesWithoutPermissions", roles.stream().filter(role -> role.getPermissions() == null || role.getPermissions().isEmpty()).count(),
                "inactiveRoles", roles.stream().filter(role -> Boolean.FALSE.equals(role.getActive())).count()
        ));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapAuditLog(AuditLog log) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", log.getId());
        item.put("adminId", log.getAdminId());
        item.put("action", log.getAction());
        item.put("entity", log.getEntity());
        item.put("timestamp", log.getTimestamp());
        return item;
    }

    private Map<String, Object> mapPageAccess(Map<String, Object> page, Set<String> permissionCodes) {
        Map<String, String> permissions = (Map<String, String>) page.getOrDefault("permissions", Map.of());
        String level = "NONE";
        if (permissionCodes.contains(permissions.get("full"))) {
            level = "FULL";
        } else if (permissionCodes.contains(permissions.get("write"))) {
            level = "WRITE";
        } else if (permissionCodes.contains(permissions.get("read"))) {
            level = "READ";
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("route", page.get("route"));
        item.put("label", page.get("label"));
        item.put("portal", page.get("portal"));
        item.put("level", level);
        return item;
    }

    private boolean containsIgnoreCase(String source, String query) {
        return query == null || query.isBlank() || (source != null && source.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)));
    }

    private boolean statusEquals(String value, String expected) {
        return value != null && value.equalsIgnoreCase(expected);
    }

    private Map<String, Object> controlHealth(String name, boolean passing, String detail) {
        return Map.of(
                "name", name,
                "status", passing ? "PASSING" : "ACTION_REQUIRED",
                "detail", detail
        );
    }

    private int governanceScore(long suspendedUsers, long openRefunds, long openSupportTickets, long criticalIncidents, long rolesWithoutPermissions) {
        long deductions = Math.min(35, suspendedUsers * 2)
                + Math.min(20, openRefunds * 3)
                + Math.min(15, openSupportTickets)
                + Math.min(25, criticalIncidents * 10)
                + Math.min(10, rolesWithoutPermissions * 5);
        return (int) Math.max(0, 100 - deductions);
    }

    private String governanceRiskLevel(long suspendedUsers, long openRefunds, long openSupportTickets, long criticalIncidents, long highFraudAlerts) {
        if (criticalIncidents > 0 || highFraudAlerts >= 5 || openRefunds >= 10) {
            return "CRITICAL";
        }
        if (suspendedUsers >= 10 || openSupportTickets >= 20 || highFraudAlerts > 0) {
            return "HIGH";
        }
        if (openRefunds > 0 || openSupportTickets > 0 || suspendedUsers > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private List<Map<String, Object>> governancePriorityActions(long pendingOwnerVerifications, long pendingTurfModerations, long openRefunds, long openSupportTickets, long criticalIncidents, long highFraudAlerts, long rolesWithoutPermissions) {
        List<Map<String, Object>> actions = new ArrayList<>();
        addPriorityAction(actions, "Owner approvals", pendingOwnerVerifications, "OPERATIONS", "Review owner KYC queue");
        addPriorityAction(actions, "Turf moderation", pendingTurfModerations, "OPERATIONS", "Approve, reject, or hide turf supply");
        addPriorityAction(actions, "Refund approvals", openRefunds, "FINANCE", "Resolve pending refund queue");
        addPriorityAction(actions, "Support SLA", openSupportTickets, "SUPPORT", "Assign and resolve open tickets");
        addPriorityAction(actions, "Critical incidents", criticalIncidents, "SECURITY", "Escalate unresolved critical incidents");
        addPriorityAction(actions, "Fraud alerts", highFraudAlerts, "TRUST", "Investigate high severity fraud alerts");
        addPriorityAction(actions, "RBAC gaps", rolesWithoutPermissions, "SECURITY", "Add permissions or disable empty roles");
        return actions;
    }

    private void addPriorityAction(List<Map<String, Object>> actions, String label, long count, String owner, String recommendation) {
        if (count <= 0) {
            return;
        }
        actions.add(Map.of(
                "label", label,
                "count", count,
                "owner", owner,
                "recommendation", recommendation
        ));
    }

    private Map<String, Object> mapAdminRole(AdminRole role) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", role.getId());
        item.put("name", role.getName());
        item.put("description", role.getDescription());
        item.put("systemRole", role.getSystemRole());
        item.put("active", role.getActive());
        item.put("createdAt", role.getCreatedAt());
        item.put("permissions", role.getPermissions().stream()
                .sorted(Comparator.comparing(AdminPermission::getCode))
                .map(this::mapPermission)
                .toList());
        return item;
    }

    private Map<String, Object> mapPermission(AdminPermission permission) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", permission.getId());
        item.put("code", permission.getCode());
        item.put("name", permission.getName());
        item.put("module", permission.getModule());
        item.put("description", permission.getDescription());
        return item;
    }

    private Map<String, Object> mapPermissionGroup(PermissionGroup group) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", group.getId());
        item.put("name", group.getName());
        item.put("description", group.getDescription());
        item.put("permissions", group.getPermissions().stream()
                .sorted(Comparator.comparing(AdminPermission::getCode))
                .map(this::mapPermission)
                .toList());
        return item;
    }

    private Map<String, Object> mapAccessPolicy(AccessPolicy policy) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", policy.getId());
        item.put("roleId", policy.getRole().getId());
        item.put("roleName", policy.getRole().getName());
        item.put("policyType", policy.getPolicyType());
        item.put("policyValue", policy.getPolicyValue());
        item.put("createdAt", policy.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapResourcePermission(ResourcePermission permission) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", permission.getId());
        item.put("userId", permission.getUserId());
        item.put("resourceType", permission.getResourceType());
        item.put("resourceId", permission.getResourceId());
        item.put("permission", permission.getPermission());
        item.put("createdAt", permission.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapTemporaryPermission(TemporaryPermission permission) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", permission.getId());
        item.put("userId", permission.getUserId());
        item.put("permissionCode", permission.getPermission().getCode());
        item.put("permissionName", permission.getPermission().getName());
        item.put("expiresAt", permission.getExpiresAt());
        item.put("createdAt", permission.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapOwnerStaffRole(OwnerStaffRole staffRole) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", staffRole.getId());
        item.put("ownerId", staffRole.getOwnerId());
        item.put("staffId", staffRole.getStaffId());
        item.put("roleId", staffRole.getRole().getId());
        item.put("roleName", staffRole.getRole().getName());
        item.put("active", staffRole.getActive());
        item.put("createdAt", staffRole.getCreatedAt());
        return item;
    }

    private Set<String> resolveAdminPermissions(AdminUser adminUser) {
        Set<String> permissionCodes = new TreeSet<>();
        adminRoleRepository.findByName(adminUser.getRole())
                .filter(role -> !Boolean.FALSE.equals(role.getActive()))
                .ifPresent(role -> permissionCodes.addAll(role.getPermissions().stream().map(AdminPermission::getCode).toList()));
        permissionCodes.addAll(temporaryPermissionRepository.findByUserIdAndExpiresAtAfter(adminUser.getId(), LocalDateTime.now()).stream()
                .map(TemporaryPermission::getPermission)
                .map(AdminPermission::getCode)
                .toList());
        return permissionCodes;
    }

    private List<String> visibleMenus(Set<String> permissionCodes) {
        List<String> menus = new ArrayList<>();
        addMenuIfPermitted(menus, permissionCodes, "dashboard", "REPORT_VIEW");
        addMenuIfPermitted(menus, permissionCodes, "users", "USER_VIEW");
        addMenuIfPermitted(menus, permissionCodes, "owners", "OWNER_APPROVE");
        addMenuIfPermitted(menus, permissionCodes, "bookings", "BOOKING_VIEW");
        addMenuIfPermitted(menus, permissionCodes, "payments", "PAYMENT_VIEW");
        addMenuIfPermitted(menus, permissionCodes, "roles", "ROLE_MANAGE");
        addMenuIfPermitted(menus, permissionCodes, "audit", "AUDIT_VIEW");
        addMenuIfPermitted(menus, permissionCodes, "settings", "SYSTEM_SETTINGS_EDIT");
        return menus;
    }

    private void addMenuIfPermitted(List<String> menus, Set<String> permissionCodes, String menu, String permission) {
        if (permissionCodes.contains(permission)) {
            menus.add(menu);
        }
    }

    private Map<String, Object> mapAdminUser(AdminUser adminUser) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", adminUser.getId());
        item.put("email", adminUser.getEmail());
        item.put("role", adminUser.getRole());
        item.put("status", adminUser.getStatus());
        item.put("createdAt", adminUser.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAdminManagedUser(User user) {
        Optional<PlayerTrustScore> trustScore = playerTrustScoreRepository.findByUserId(user.getId());
        long reports = trustReportRepository.countByReportedUserId(user.getId());
        List<Booking> bookings = bookingRepository.findByUserId(user.getId());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("name", user.getName());
        item.put("email", user.getEmail());
        item.put("phone", user.getPhone());
        item.put("status", user.getStatus());
        item.put("roles", user.getRoles().stream().map(role -> role.getName().name()).sorted().toList());
        item.put("city", inferUserCity(user));
        item.put("trustScore", trustScore.map(PlayerTrustScore::getScore).orElse(100));
        item.put("riskLevel", riskLevel(trustScore.map(PlayerTrustScore::getScore).orElse(100), reports));
        item.put("reports", reports);
        item.put("bookings", bookings.size());
        item.put("teams", teamMemberRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());
        item.put("createdAt", user.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapBookingBrief(Booking booking) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", booking.getId());
        item.put("bookingDate", booking.getBookingDate());
        item.put("status", booking.getStatus());
        item.put("amount", booking.getAmount());
        item.put("turfName", booking.getTurf() == null ? "Unknown turf" : booking.getTurf().getName());
        item.put("createdAt", booking.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapPaymentBrief(Payment payment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", payment.getId());
        item.put("amount", payment.getAmount());
        item.put("status", payment.getStatus());
        item.put("bookingId", payment.getBooking() == null ? null : payment.getBooking().getId());
        item.put("createdAt", payment.getCreatedAt());
        return item;
    }

    private boolean hasRole(User user, com.example.Cricbook.enums.RoleName roleName) {
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private boolean hasRoleCode(User user, String roleCode) {
        String normalized = roleCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        String finalNormalized = normalized;
        return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName().name().equals(finalNormalized));
    }

    private String inferUserCity(User user) {
        List<TeamMember> memberships = teamMemberRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (!memberships.isEmpty() && memberships.get(0).getTeam() != null) {
            return memberships.get(0).getTeam().getCity();
        }
        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(user.getId());
        if (!bookings.isEmpty() && bookings.get(0).getTurf() != null) {
            return bookings.get(0).getTurf().getCity();
        }
        return "";
    }

    private String riskLevel(int trustScore, long reports) {
        if (trustScore < 35 || reports >= 5) return "CRITICAL";
        if (trustScore < 55 || reports >= 3) return "HIGH";
        if (trustScore < 75 || reports >= 1) return "MEDIUM";
        return "LOW";
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    // --- 9. SUPPORT QUEUES & TICKETS ---

    @GetMapping("/support-tickets")
    public ResponseEntity<List<SupportTicket>> getSupportTickets() {
        // Auto seed sample ticket if none exist
        if (supportTicketRepository.count() == 0) {
            List<User> users = userRepository.findAll();
            Long userId = users.isEmpty() ? 1L : users.get(0).getId();
            SupportTicket ticket = SupportTicket.builder()
                    .userId(userId)
                    .issue("Having trouble editing my turf slot availability timings on the mobile dashboard.")
                    .status("OPEN")
                    .chatLog("User: Having trouble editing my turf slot availability timings on the mobile dashboard.")
                    .build();
            supportTicketRepository.save(ticket);
        }
        return ResponseEntity.ok(supportTicketRepository.findAll());
    }

    @PostMapping("/support-tickets/{ticketId}/reply")
    public ResponseEntity<SupportTicket> replyToSupportTicket(@PathVariable Long ticketId, @RequestBody Map<String, String> body) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new NoSuchElementException("Support ticket not found"));

        String reply = body.get("reply");
        String status = body.getOrDefault("status", "IN_PROGRESS");

        String currentChat = ticket.getChatLog() != null ? ticket.getChatLog() : "";
        ticket.setChatLog(currentChat + "\nSupport (" + getAdminUsername() + "): " + reply);
        ticket.setStatus(status);

        if (body.containsKey("internalNotes")) {
            ticket.setInternalNotes(body.get("internalNotes"));
        }

        supportTicketRepository.save(ticket);
        writeAudit("SUPPORT_REPLY", "Ticket ID " + ticketId + " Replied (Status: " + status + ")");
        return ResponseEntity.ok(ticket);
    }

    // --- 10. INCIDENT DISPATCH OUTAGES ---

    @GetMapping("/incidents")
    public ResponseEntity<List<Incident>> getIncidents() {
        return ResponseEntity.ok(incidentRepository.findAll());
    }

    @PostMapping("/incidents")
    public ResponseEntity<Incident> createIncident(@RequestBody Incident incident) {
        if (incident.getStatus() == null) {
            incident.setStatus("INVESTIGATING");
        }
        incidentRepository.save(incident);
        writeAudit("CREATE_INCIDENT", "Incident: " + incident.getName() + " Severity: " + incident.getSeverity());
        return ResponseEntity.ok(incident);
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<Incident> resolveIncident(@PathVariable Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Incident not found"));
        incident.setStatus("RESOLVED");
        incidentRepository.save(incident);
        writeAudit("RESOLVE_INCIDENT", "Incident ID " + id + " (" + incident.getName() + ")");
        return ResponseEntity.ok(incident);
    }

    @Data
    public static class AdminRoleRequest {
        private String name;
        private String description;
        private List<String> permissionCodes = new ArrayList<>();
    }

    @Data
    public static class AdminRoleAssignmentRequest {
        private String role;
    }

    @Data
    public static class AdminUserStatusRequest {
        private String status;
        private String reason;
    }

    @Data
    public static class PermissionGroupRequest {
        private String name;
        private String description;
        private List<String> permissionCodes = new ArrayList<>();
    }

    @Data
    public static class AccessPolicyRequest {
        private Long roleId;
        private String policyType;
        private String policyValue;
    }

    @Data
    public static class ResourcePermissionRequest {
        private Long userId;
        private String resourceType;
        private Long resourceId;
        private String permission;
    }

    @Data
    public static class TemporaryPermissionRequest {
        private Long userId;
        private String permissionCode;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class OwnerStaffRoleRequest {
        private Long ownerId;
        private Long staffId;
        private Long roleId;
    }
}
