package com.example.Cricbook.controller;

import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.TeamVisibility;
import com.example.Cricbook.enums.TrustReportStatus;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/safety")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('REPORT_VIEW')")
@Transactional
public class AdminSafetyController {

    private final TrustReportRepository trustReportRepository;
    private final PlayerTrustScoreRepository playerTrustScoreRepository;
    private final UserRepository userRepository;
    private final UserWarningRepository userWarningRepository;
    private final SafetySuspensionRepository safetySuspensionRepository;
    private final BannedEntityRepository bannedEntityRepository;
    private final ModerationAppealRepository moderationAppealRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaReviewRepository mediaReviewRepository;
    private final FraudProfileRepository fraudProfileRepository;
    private final TeamRepository teamRepository;
    private final TeamModerationRepository teamModerationRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewModerationRepository reviewModerationRepository;
    private final CommunityModerationRepository communityModerationRepository;
    private final TrustAdjustmentRepository trustAdjustmentRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        List<TrustReport> reports = trustReportRepository.findAll();
        List<MediaAsset> media = mediaAssetRepository.findAll();
        List<PlayerTrustScore> scores = playerTrustScoreRepository.findAll();
        long openReports = reports.stream().filter(report -> report.getStatus() == TrustReportStatus.OPEN).count();
        long flaggedMedia = media.stream()
                .filter(asset -> !"APPROVED".equalsIgnoreCase(safe(asset.getModerationStatus())) || !Boolean.TRUE.equals(asset.getActive()))
                .count();
        long trustViolations = scores.stream().filter(score -> optionalScore(score.getScore()) < 50).count();
        long pendingAppeals = moderationAppealRepository.findByStatusOrderByCreatedAtDesc("PENDING").size();
        long suspendedUsers = safetySuspensionRepository.findByStatusOrderByCreatedAtDesc("ACTIVE").stream()
                .filter(item -> "USER".equalsIgnoreCase(item.getEntityType()))
                .count();
        long fraudCases = fraudProfileRepository.findByStatusOrderByCreatedAtDesc("OPEN").size();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("openReports", openReports);
        response.put("pendingAppeals", pendingAppeals);
        response.put("suspendedUsers", suspendedUsers);
        response.put("flaggedPhotos", flaggedMedia);
        response.put("fraudCases", fraudCases);
        response.put("trustViolations", trustViolations);
        response.put("warnings", userWarningRepository.count());
        response.put("bans", bannedEntityRepository.findByActiveTrueOrderByCreatedAtDesc().size());
        response.put("safetyHealthScore", safetyHealthScore(openReports, pendingAppeals, fraudCases, trustViolations));
        response.put("reportsPerDay", reportsPerDay(reports));
        response.put("insight", safetyInsight(openReports, pendingAppeals, fraudCases, trustViolations));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<Map<String, Object>>> reports() {
        return ResponseEntity.ok(trustReportRepository.findAll().stream()
                .sorted(Comparator.comparing(TrustReport::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapReport)
                .toList());
    }

    @PostMapping("/reports/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('REPORT_RESOLVE')")
    public ResponseEntity<Map<String, Object>> resolveReport(@PathVariable Long id, @RequestBody ResolveReportRequest request) {
        TrustReport report = trustReportRepository.findById(id).orElseThrow();
        report.setStatus(TrustReportStatus.valueOf(defaultString(request.getStatus(), "ACTIONED").toUpperCase(Locale.ROOT)));
        TrustReport saved = trustReportRepository.save(report);
        if (request.getTrustScore() != null && saved.getReportedUser() != null) {
            adjustTrustScore(saved.getReportedUser(), request.getTrustScore(), defaultString(request.getReason(), "Report moderation action"));
        }
        return ResponseEntity.ok(mapReport(saved));
    }

    @GetMapping("/warnings")
    public ResponseEntity<List<Map<String, Object>>> warnings() {
        return ResponseEntity.ok(userWarningRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapWarning).toList());
    }

    @PostMapping("/warnings")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_WARN')")
    public ResponseEntity<Map<String, Object>> warn(@RequestBody WarningRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow();
        UserWarning warning = userWarningRepository.save(UserWarning.builder()
                .user(user)
                .warningLevel(defaultString(request.getWarningLevel(), "LEVEL_1"))
                .reason(defaultString(request.getReason(), "Policy warning"))
                .issuedBy(actor())
                .build());
        return ResponseEntity.ok(mapWarning(warning));
    }

    @GetMapping("/suspensions")
    public ResponseEntity<List<Map<String, Object>>> suspensions() {
        return ResponseEntity.ok(safetySuspensionRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapSuspension).toList());
    }

    @PostMapping("/suspensions")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_SUSPEND')")
    public ResponseEntity<Map<String, Object>> suspend(@RequestBody SuspensionRequest request) {
        SafetySuspension suspension = safetySuspensionRepository.save(SafetySuspension.builder()
                .entityType(defaultString(request.getEntityType(), "USER").toUpperCase(Locale.ROOT))
                .entityId(request.getEntityId())
                .startDate(LocalDateTime.now())
                .endDate(request.getHours() == null ? null : LocalDateTime.now().plusHours(request.getHours()))
                .status("ACTIVE")
                .reason(defaultString(request.getReason(), "Safety suspension"))
                .build());
        if ("USER".equalsIgnoreCase(suspension.getEntityType())) {
            userRepository.findById(suspension.getEntityId()).ifPresent(user -> {
                user.setStatus(UserStatus.BLOCKED);
                userRepository.save(user);
            });
        }
        return ResponseEntity.ok(mapSuspension(suspension));
    }

    @PostMapping("/suspensions/{id}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_SUSPEND')")
    public ResponseEntity<Map<String, Object>> resolveSuspension(@PathVariable Long id) {
        SafetySuspension suspension = safetySuspensionRepository.findById(id).orElseThrow();
        suspension.setStatus("RESOLVED");
        if ("USER".equalsIgnoreCase(suspension.getEntityType())) {
            userRepository.findById(suspension.getEntityId()).ifPresent(user -> {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
            });
        }
        return ResponseEntity.ok(mapSuspension(safetySuspensionRepository.save(suspension)));
    }

    @GetMapping("/bans")
    public ResponseEntity<List<Map<String, Object>>> bans() {
        return ResponseEntity.ok(bannedEntityRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapBan).toList());
    }

    @PostMapping("/bans")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_BAN')")
    public ResponseEntity<Map<String, Object>> ban(@RequestBody BanRequest request) {
        BannedEntity ban = bannedEntityRepository.save(BannedEntity.builder()
                .type(defaultString(request.getType(), "ACCOUNT").toUpperCase(Locale.ROOT))
                .value(defaultString(request.getValue(), ""))
                .reason(defaultString(request.getReason(), "Safety ban"))
                .active(true)
                .build());
        findUserByValue(ban.getValue()).ifPresent(user -> {
            user.setStatus(UserStatus.BLOCKED);
            userRepository.save(user);
        });
        return ResponseEntity.ok(mapBan(ban));
    }

    @PostMapping("/bans/{id}/unban")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('USER_BAN')")
    public ResponseEntity<Map<String, Object>> unban(@PathVariable Long id) {
        BannedEntity ban = bannedEntityRepository.findById(id).orElseThrow();
        ban.setActive(false);
        findUserByValue(ban.getValue()).ifPresent(user -> {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        });
        return ResponseEntity.ok(mapBan(bannedEntityRepository.save(ban)));
    }

    @GetMapping("/appeals")
    public ResponseEntity<List<Map<String, Object>>> appeals() {
        return ResponseEntity.ok(moderationAppealRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapAppeal).toList());
    }

    @PostMapping("/appeals")
    public ResponseEntity<Map<String, Object>> createAppeal(@RequestBody AppealRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow();
        ModerationAppeal appeal = moderationAppealRepository.save(ModerationAppeal.builder()
                .user(user)
                .caseId(request.getCaseId())
                .caseType(defaultString(request.getCaseType(), "SAFETY_CASE"))
                .message(defaultString(request.getMessage(), "Appeal submitted"))
                .status("PENDING")
                .build());
        return ResponseEntity.ok(mapAppeal(appeal));
    }

    @PostMapping("/appeals/{id}/decision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('APPEAL_REVIEW')")
    public ResponseEntity<Map<String, Object>> decideAppeal(@PathVariable Long id, @RequestBody DecisionRequest request) {
        ModerationAppeal appeal = moderationAppealRepository.findById(id).orElseThrow();
        appeal.setStatus(defaultString(request.getDecision(), "REJECTED").toUpperCase(Locale.ROOT));
        appeal.setDecisionReason(defaultString(request.getReason(), "Reviewed by Trust & Safety"));
        appeal.setReviewedBy(actor());
        return ResponseEntity.ok(mapAppeal(moderationAppealRepository.save(appeal)));
    }

    @GetMapping("/media")
    public ResponseEntity<List<Map<String, Object>>> media() {
        return ResponseEntity.ok(mediaAssetRepository.findAll().stream()
                .sorted(Comparator.comparing(MediaAsset::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapMedia)
                .toList());
    }

    @PostMapping("/media/{id}/decision")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('MEDIA_MODERATE')")
    public ResponseEntity<Map<String, Object>> decideMedia(@PathVariable Long id, @RequestBody DecisionRequest request) {
        MediaAsset asset = mediaAssetRepository.findById(id).orElseThrow();
        String decision = defaultString(request.getDecision(), "APPROVED").toUpperCase(Locale.ROOT);
        asset.setModerationStatus(decision);
        asset.setModerationReason(defaultString(request.getReason(), "Reviewed by Trust & Safety"));
        if ("REJECTED".equals(decision) || "REMOVED".equals(decision)) {
            asset.setActive(false);
        }
        MediaAsset saved = mediaAssetRepository.save(asset);
        mediaReviewRepository.save(MediaReview.builder()
                .media(saved)
                .status(decision)
                .reviewedBy(actor())
                .reason(defaultString(request.getReason(), "Media decision"))
                .build());
        return ResponseEntity.ok(mapMedia(saved));
    }

    @GetMapping("/fraud-profiles")
    public ResponseEntity<List<Map<String, Object>>> fraudProfiles() {
        return ResponseEntity.ok(fraudProfileRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapFraud).toList());
    }

    @PostMapping("/fraud-profiles/scan")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('REPORT_RESOLVE')")
    public ResponseEntity<Map<String, Object>> scanFraudProfiles() {
        int created = 0;
        for (PlayerTrustScore score : playerTrustScoreRepository.findAll()) {
            if (score.getUser() == null || fraudProfileRepository.existsByUserIdAndStatus(score.getUser().getId(), "OPEN")) {
                continue;
            }
            long reportCount = trustReportRepository.countByReportedUserId(score.getUser().getId());
            int trust = optionalScore(score.getScore());
            if (trust < 45 || reportCount >= 3) {
                fraudProfileRepository.save(FraudProfile.builder()
                        .user(score.getUser())
                        .riskLevel(trust < 30 || reportCount >= 5 ? "CRITICAL" : "HIGH")
                        .reason("Automated safety scan flagged low trust or repeated reports")
                        .signals("trustScore=" + trust + ", reports=" + reportCount)
                        .status("OPEN")
                        .build());
                created++;
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("created", created);
        response.put("fraudProfiles", fraudProfileRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapFraud).toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Map<String, Object>>> teams() {
        return ResponseEntity.ok(teamRepository.findAll().stream()
                .sorted(Comparator.comparing(Team::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapTeam)
                .toList());
    }

    @PostMapping("/teams/{id}/moderate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('COMMUNITY_MODERATE')")
    public ResponseEntity<Map<String, Object>> moderateTeam(@PathVariable Long id, @RequestBody DecisionRequest request) {
        Team team = teamRepository.findById(id).orElseThrow();
        String action = defaultString(request.getDecision(), "HIDE").toUpperCase(Locale.ROOT);
        if ("HIDE".equals(action) || "SUSPEND".equals(action)) {
            team.setVisibility(TeamVisibility.PRIVATE);
            teamRepository.save(team);
        }
        teamModerationRepository.save(TeamModeration.builder()
                .team(team)
                .action(action)
                .reason(defaultString(request.getReason(), "Team moderation action"))
                .moderatorId(actor())
                .build());
        return ResponseEntity.ok(mapTeam(team));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Map<String, Object>>> reviews() {
        return ResponseEntity.ok(reviewRepository.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapReview)
                .toList());
    }

    @PostMapping("/reviews/{id}/moderate")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('COMMUNITY_MODERATE')")
    public ResponseEntity<Map<String, Object>> moderateReview(@PathVariable Long id, @RequestBody DecisionRequest request) {
        Review review = reviewRepository.findById(id).orElseThrow();
        String action = defaultString(request.getDecision(), "INVESTIGATE").toUpperCase(Locale.ROOT);
        reviewModerationRepository.save(ReviewModeration.builder()
                .reviewId(review.getId())
                .action(action)
                .moderatorId(actor())
                .reason(defaultString(request.getReason(), "Review moderation action"))
                .build());
        if ("DELETE".equals(action)) {
            reviewRepository.delete(review);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", id);
            response.put("action", "DELETE");
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(mapReview(review));
    }

    @GetMapping("/trust/adjustments")
    public ResponseEntity<List<Map<String, Object>>> trustAdjustments() {
        return ResponseEntity.ok(trustAdjustmentRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapTrustAdjustment).toList());
    }

    @PostMapping("/trust/{userId}/adjust")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('TRUST_EDIT')")
    public ResponseEntity<Map<String, Object>> adjustTrust(@PathVariable Long userId, @RequestBody TrustAdjustRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        adjustTrustScore(user, request.getNewScore(), defaultString(request.getReason(), "Manual trust adjustment"));
        return ResponseEntity.ok(playerTrustScoreRepository.findByUserId(userId).map(this::mapTrustScore).orElseGet(LinkedHashMap::new));
    }

    @GetMapping("/community/moderation")
    public ResponseEntity<List<Map<String, Object>>> communityModeration() {
        return ResponseEntity.ok(communityModerationRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::mapCommunityModeration).toList());
    }

    @PostMapping("/community/moderation")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('COMMUNITY_MODERATE')")
    public ResponseEntity<Map<String, Object>> moderateCommunity(@RequestBody CommunityModerationRequest request) {
        CommunityModeration record = communityModerationRepository.save(CommunityModeration.builder()
                .contentType(defaultString(request.getContentType(), "POST"))
                .contentId(request.getContentId())
                .action(defaultString(request.getAction(), "HIDE").toUpperCase(Locale.ROOT))
                .moderatorId(actor())
                .reason(defaultString(request.getReason(), "Community moderation action"))
                .build());
        return ResponseEntity.ok(mapCommunityModeration(record));
    }

    private void adjustTrustScore(User user, Integer newScore, String reason) {
        if (newScore == null) {
            return;
        }
        PlayerTrustScore score = playerTrustScoreRepository.findByUserId(user.getId())
                .orElseGet(() -> PlayerTrustScore.builder()
                        .user(user)
                        .score(100)
                        .attendanceScore(100)
                        .punctualityScore(100)
                        .cancellationScore(100)
                        .reportScore(100)
                        .paymentScore(100)
                        .matchCompletionScore(100)
                        .build());
        int oldScore = optionalScore(score.getScore());
        int bounded = Math.max(0, Math.min(100, newScore));
        score.setScore(bounded);
        playerTrustScoreRepository.save(score);
        trustAdjustmentRepository.save(TrustAdjustment.builder()
                .user(user)
                .oldScore(oldScore)
                .newScore(bounded)
                .reason(reason)
                .adminId(actor())
                .build());
    }

    private Map<String, Object> mapReport(TrustReport report) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("reporter", report.getReporter() == null ? "Unknown" : report.getReporter().getName());
        item.put("reportedUserId", report.getReportedUser() == null ? null : report.getReportedUser().getId());
        item.put("reportedUser", report.getReportedUser() == null ? "Unknown" : report.getReportedUser().getName());
        item.put("reason", report.getReason() == null ? "OTHER" : report.getReason().name());
        item.put("description", report.getDescription());
        item.put("status", report.getStatus() == null ? "OPEN" : report.getStatus().name());
        item.put("createdAt", report.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapWarning(UserWarning warning) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", warning.getId());
        item.put("userId", warning.getUser().getId());
        item.put("userName", warning.getUser().getName());
        item.put("warningLevel", warning.getWarningLevel());
        item.put("reason", warning.getReason());
        item.put("issuedBy", warning.getIssuedBy());
        item.put("createdAt", warning.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapSuspension(SafetySuspension suspension) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", suspension.getId());
        item.put("entityType", suspension.getEntityType());
        item.put("entityId", suspension.getEntityId());
        item.put("status", suspension.getStatus());
        item.put("reason", suspension.getReason());
        item.put("startDate", suspension.getStartDate());
        item.put("endDate", suspension.getEndDate());
        return item;
    }

    private Map<String, Object> mapBan(BannedEntity ban) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", ban.getId());
        item.put("type", ban.getType());
        item.put("value", ban.getValue());
        item.put("reason", ban.getReason());
        item.put("active", ban.isActive());
        item.put("createdAt", ban.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAppeal(ModerationAppeal appeal) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", appeal.getId());
        item.put("userId", appeal.getUser().getId());
        item.put("userName", appeal.getUser().getName());
        item.put("caseId", appeal.getCaseId());
        item.put("caseType", appeal.getCaseType());
        item.put("message", appeal.getMessage());
        item.put("status", appeal.getStatus());
        item.put("decisionReason", appeal.getDecisionReason());
        item.put("createdAt", appeal.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapMedia(MediaAsset asset) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", asset.getId());
        item.put("ownerType", asset.getOwnerType());
        item.put("ownerId", asset.getOwnerId());
        item.put("mediaType", asset.getMediaType());
        item.put("imageUrl", asset.getImageUrl());
        item.put("moderationStatus", asset.getModerationStatus());
        item.put("moderationReason", asset.getModerationReason());
        item.put("active", asset.getActive());
        item.put("uploadedAt", asset.getUploadedAt());
        return item;
    }

    private Map<String, Object> mapFraud(FraudProfile profile) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", profile.getId());
        item.put("userId", profile.getUser().getId());
        item.put("userName", profile.getUser().getName());
        item.put("riskLevel", profile.getRiskLevel());
        item.put("reason", profile.getReason());
        item.put("signals", profile.getSignals());
        item.put("status", profile.getStatus());
        item.put("createdAt", profile.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapTeam(Team team) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", team.getId());
        item.put("name", team.getName());
        item.put("sport", team.getSport());
        item.put("city", team.getCity());
        item.put("visibility", team.getVisibility() == null ? "PUBLIC" : team.getVisibility().name());
        item.put("captain", team.getCaptain() == null ? "Unknown" : team.getCaptain().getName());
        item.put("createdAt", team.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapReview(Review review) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", review.getId());
        item.put("userId", review.getUserId());
        item.put("turfId", review.getTurfId());
        item.put("rating", review.getRating());
        item.put("comment", review.getComment());
        item.put("reports", reviewReportRepository.findByReviewId(review.getId()).size());
        item.put("createdAt", review.getCreatedAt());
        return item;
    }

    private LinkedHashMap<String, Object> mapTrustScore(PlayerTrustScore score) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("userId", score.getUser() == null ? null : score.getUser().getId());
        item.put("userName", score.getUser() == null ? "Unknown" : score.getUser().getName());
        item.put("score", score.getScore());
        item.put("updatedAt", score.getUpdatedAt());
        return item;
    }

    private Map<String, Object> mapTrustAdjustment(TrustAdjustment adjustment) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", adjustment.getId());
        item.put("userId", adjustment.getUser().getId());
        item.put("userName", adjustment.getUser().getName());
        item.put("oldScore", adjustment.getOldScore());
        item.put("newScore", adjustment.getNewScore());
        item.put("reason", adjustment.getReason());
        item.put("adminId", adjustment.getAdminId());
        item.put("createdAt", adjustment.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapCommunityModeration(CommunityModeration moderation) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", moderation.getId());
        item.put("contentType", moderation.getContentType());
        item.put("contentId", moderation.getContentId());
        item.put("action", moderation.getAction());
        item.put("reason", moderation.getReason());
        item.put("moderatorId", moderation.getModeratorId());
        item.put("createdAt", moderation.getCreatedAt());
        return item;
    }

    private List<Map<String, Object>> reportsPerDay(List<TrustReport> reports) {
        Map<String, Long> buckets = new TreeMap<>();
        reports.stream()
                .filter(report -> report.getCreatedAt() != null)
                .forEach(report -> buckets.merge(report.getCreatedAt().toLocalDate().toString(), 1L, Long::sum));
        return buckets.entrySet().stream().map(entry -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", entry.getKey());
            item.put("reports", entry.getValue());
            return item;
        }).toList();
    }

    private Optional<User> findUserByValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return userRepository.findById(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return userRepository.findByEmail(value);
        }
    }

    private String actor() {
        return SecurityContextHolder.getContext().getAuthentication() == null
                ? "system"
                : SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private int safetyHealthScore(long openReports, long pendingAppeals, long fraudCases, long trustViolations) {
        long penalty = openReports * 3 + pendingAppeals * 2 + fraudCases * 5 + trustViolations * 4;
        return (int) Math.max(0, 100 - penalty);
    }

    private String safetyInsight(long openReports, long pendingAppeals, long fraudCases, long trustViolations) {
        if (fraudCases > 0) {
            return "Fraud review queue needs attention before user growth campaigns.";
        }
        if (openReports > 5 || pendingAppeals > 3) {
            return "Moderation backlog is rising; prioritize report and appeal decisions.";
        }
        if (trustViolations > 0) {
            return "Low-trust accounts exist; review warnings or temporary suspensions.";
        }
        return "Safety queues are stable and no major moderation backlog is visible.";
    }

    private int optionalScore(Integer score) {
        return score == null ? 100 : score;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    public static class ResolveReportRequest {
        private String status;
        private Integer trustScore;
        private String reason;
    }

    @Data
    public static class WarningRequest {
        private Long userId;
        private String warningLevel;
        private String reason;
    }

    @Data
    public static class SuspensionRequest {
        private String entityType;
        private Long entityId;
        private Long hours;
        private String reason;
    }

    @Data
    public static class BanRequest {
        private String type;
        private String value;
        private String reason;
    }

    @Data
    public static class AppealRequest {
        private Long userId;
        private Long caseId;
        private String caseType;
        private String message;
    }

    @Data
    public static class DecisionRequest {
        private String decision;
        private String reason;
    }

    @Data
    public static class TrustAdjustRequest {
        private Integer newScore;
        private String reason;
    }

    @Data
    public static class CommunityModerationRequest {
        private String contentType;
        private Long contentId;
        private String action;
        private String reason;
    }
}
