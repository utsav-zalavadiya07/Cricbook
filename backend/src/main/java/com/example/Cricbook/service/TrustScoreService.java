package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrustScoreService {
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PlayerTrustScoreRepository trustScoreRepository;
    private final TrustAttendanceRepository attendanceRepository;
    private final PunctualityLogRepository punctualityLogRepository;
    private final CancellationHistoryRepository cancellationHistoryRepository;
    private final TrustReportRepository trustReportRepository;
    private final TrustRiskRepository trustRiskRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public TrustScoreResponse getMyProfile(String loginId) {
        User user = findUser(loginId);
        return mapScore(ensureScoreReadOnly(user));
    }

    @Transactional(readOnly = true)
    public TrustScoreResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Player account was not found."));
        return mapScore(ensureScoreReadOnly(user));
    }

    @Transactional
    public TrustScoreResponse calculateScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Player account was not found."));
        return mapScore(recalculate(user));
    }

    @Transactional
    public TrustAttendanceResponse updateAttendance(String ownerLogin, Long bookingId, TrustAttendanceRequest request) {
        if (request == null || request.getStatus() == null) {
            throw ApiException.badRequest("Attendance status is required.");
        }

        User owner = findUser(ownerLogin);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        validateOwnerOrAdmin(owner, booking);

        LocalDateTime checkinTime = request.getCheckinTime() == null ? LocalDateTime.now() : request.getCheckinTime();
        TrustAttendance attendance = attendanceRepository.findByBookingId(bookingId)
                .orElse(TrustAttendance.builder()
                        .booking(booking)
                        .user(booking.getUser())
                        .build());
        attendance.setStatus(request.getStatus());
        attendance.setCheckinTime(checkinTime);
        TrustAttendance saved = attendanceRepository.save(attendance);

        if (request.getStatus() == AttendanceStatus.ARRIVED || request.getStatus() == AttendanceStatus.LATE) {
            int delay = calculateDelayMinutes(booking, checkinTime);
            String impact = delay < 5 ? "NO_PENALTY" : delay <= 15 ? "SMALL_PENALTY" : "HIGH_PENALTY";
            punctualityLogRepository.save(PunctualityLog.builder()
                    .user(booking.getUser())
                    .attendance(saved)
                    .delayMinutes(Math.max(delay, 0))
                    .impact(impact)
                    .build());
            if (delay > 15) {
                saved.setStatus(AttendanceStatus.LATE);
            }
        }

        PlayerTrustScore next = recalculate(booking.getUser());
        if (next.getScore() >= 90) {
            notificationService.pushNotification(
                    booking.getUser().getId(),
                    booking.getUser().getEmail(),
                    "Trusted Badge Earned",
                    "Your reliable attendance improved your Cricbook trust profile."
            );
        }
        return mapAttendance(saved);
    }

    @Transactional
    public TrustReportResponse submitReport(String reporterLogin, TrustReportRequest request) {
        if (request == null || request.getReportedUserId() == null || request.getReason() == null) {
            throw ApiException.badRequest("Reported player and reason are required.");
        }
        User reporter = findUser(reporterLogin);
        User reported = userRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> ApiException.notFound("Reported player was not found."));
        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> ApiException.notFound("Booking was not found."));
            validateReportAccess(reporter, booking);
        }
        TrustReport report = trustReportRepository.save(TrustReport.builder()
                .reporter(reporter)
                .reportedUser(reported)
                .booking(booking)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(TrustReportStatus.OPEN)
                .build());
        recalculate(reported);
        return mapReport(report);
    }

    @Transactional
    public TrustReportResponse moderateReport(Long reportId, TrustModerationRequest request) {
        TrustReport report = trustReportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("Trust report was not found."));
        TrustReportStatus nextStatus = request == null || request.getStatus() == null ? TrustReportStatus.REVIEWED : request.getStatus();
        report.setStatus(nextStatus);
        TrustReport saved = trustReportRepository.save(report);
        if (request != null && request.getScoreAdjustment() != null && request.getScoreAdjustment() != 0) {
            applyScoreAdjustment(report.getReportedUser(), request.getScoreAdjustment(), request.getReason());
        } else {
            recalculate(report.getReportedUser());
        }
        return mapReport(saved);
    }

    @Transactional(readOnly = true)
    public List<TrustReportResponse> openReports() {
        return trustReportRepository.findByStatusOrderByCreatedAtDesc(TrustReportStatus.OPEN)
                .stream()
                .map(this::mapReport)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrustAttendanceResponse> attendanceHistory(Long userId) {
        return attendanceRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapAttendance)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrustAnalyticsResponse analytics() {
        List<TrustScoreResponse> profiles = trustScoreRepository.findAll().stream().map(this::mapScore).toList();
        return TrustAnalyticsResponse.builder()
                .totalProfiles(profiles.size())
                .elitePlayers(profiles.stream().filter(score -> score.getScore() >= 95).count())
                .trustedPlayers(profiles.stream().filter(score -> score.getScore() >= 80).count())
                .riskyPlayers(profiles.stream().filter(score -> score.getScore() < 60).count())
                .openReports(trustReportRepository.countByStatus(TrustReportStatus.OPEN))
                .riskiestPlayers(profiles.stream().sorted(Comparator.comparing(TrustScoreResponse::getScore)).limit(8).toList())
                .mostReliablePlayers(profiles.stream().sorted(Comparator.comparing(TrustScoreResponse::getScore).reversed()).limit(8).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrustScoreResponse> ownerRiskBoard(String ownerLogin) {
        User owner = findUser(ownerLogin);
        return bookingRepository.findByTurfOwnerId(owner.getId()).stream()
                .map(Booking::getUser)
                .distinct()
                .map(user -> mapScore(ensureScoreReadOnly(user)))
                .sorted(Comparator.comparing(TrustScoreResponse::getScore))
                .limit(12)
                .toList();
    }

    @Transactional
    public void recordCancellation(Booking booking) {
        if (booking == null || cancellationHistoryRepository.findByBookingId(booking.getId()).isPresent()) {
            return;
        }
        int penalty = cancellationPenalty(booking);
        cancellationHistoryRepository.save(CancellationHistory.builder()
                .booking(booking)
                .user(booking.getUser())
                .cancelledAt(LocalDateTime.now())
                .penalty(penalty)
                .build());
        recalculate(booking.getUser());
    }

    private PlayerTrustScore recalculate(User user) {
        long totalAttendance = attendanceRepository.countByUserId(user.getId());
        long noShows = attendanceRepository.countByUserIdAndStatus(user.getId(), AttendanceStatus.NO_SHOW);
        long late = attendanceRepository.countByUserIdAndStatus(user.getId(), AttendanceStatus.LATE);
        long cancellations = cancellationHistoryRepository.countByUserId(user.getId());
        long reports = trustReportRepository.countByReportedUserId(user.getId());

        int attendanceScore = totalAttendance == 0 ? 85 : clamp(100 - (int) ((noShows * 28 + late * 8) / Math.max(totalAttendance, 1)));
        int punctualityScore = totalAttendance == 0 ? 85 : clamp(100 - (int) ((late * 15) / Math.max(totalAttendance, 1)));
        int cancellationScore = clamp(100 - (int) Math.min(70, cancellations * 12));
        int reportScore = clamp(100 - (int) Math.min(80, reports * 18));
        int paymentScore = 90;
        int matchCompletionScore = clamp(100 - (int) Math.min(50, noShows * 12 + cancellations * 5));

        int weighted = (int) Math.round(
                attendanceScore * 0.30
                        + cancellationScore * 0.25
                        + punctualityScore * 0.15
                        + paymentScore * 0.10
                        + reportScore * 0.10
                        + matchCompletionScore * 0.10
        );

        PlayerTrustScore score = trustScoreRepository.findByUserId(user.getId())
                .orElse(PlayerTrustScore.builder().user(user).build());
        score.setAttendanceScore(attendanceScore);
        score.setPunctualityScore(punctualityScore);
        score.setCancellationScore(cancellationScore);
        score.setReportScore(reportScore);
        score.setPaymentScore(paymentScore);
        score.setMatchCompletionScore(matchCompletionScore);
        score.setScore(clamp(weighted));
        PlayerTrustScore saved = trustScoreRepository.save(score);
        updateRisk(user, saved, noShows, reports, cancellations);
        return saved;
    }

    private PlayerTrustScore ensureScoreReadOnly(User user) {
        return trustScoreRepository.findByUserId(user.getId())
                .orElse(PlayerTrustScore.builder()
                        .user(user)
                        .score(85)
                        .attendanceScore(85)
                        .punctualityScore(85)
                        .cancellationScore(90)
                        .reportScore(95)
                        .paymentScore(90)
                        .matchCompletionScore(85)
                        .build());
    }

    private void updateRisk(User user, PlayerTrustScore score, long noShows, long reports, long cancellations) {
        TrustRiskLevel level = score.getScore() < 45 || noShows >= 3 || reports >= 3 ? TrustRiskLevel.HIGH : score.getScore() < 70 || cancellations >= 3 ? TrustRiskLevel.MEDIUM : TrustRiskLevel.LOW;
        String reason = switch (level) {
            case HIGH -> "Repeated no-shows, reports, or very low trust score require manual review.";
            case MEDIUM -> "Recent reliability pattern needs owner caution.";
            case LOW -> "Player currently has a healthy trust profile.";
        };
        TrustRisk risk = trustRiskRepository.findByUserId(user.getId()).orElse(TrustRisk.builder().user(user).build());
        risk.setRiskLevel(level);
        risk.setReason(reason);
        trustRiskRepository.save(risk);
    }

    private void applyScoreAdjustment(User user, int adjustment, String reason) {
        PlayerTrustScore current = recalculate(user);
        current.setScore(clamp(current.getScore() + adjustment));
        trustScoreRepository.save(current);
        TrustRisk risk = trustRiskRepository.findByUserId(user.getId()).orElse(TrustRisk.builder().user(user).build());
        risk.setRiskLevel(current.getScore() < 45 ? TrustRiskLevel.HIGH : current.getScore() < 70 ? TrustRiskLevel.MEDIUM : TrustRiskLevel.LOW);
        risk.setReason(reason == null || reason.isBlank() ? "Manual moderation adjustment." : reason);
        trustRiskRepository.save(risk);
    }

    private TrustScoreResponse mapScore(PlayerTrustScore score) {
        TrustRisk risk = trustRiskRepository.findByUserId(score.getUser().getId()).orElse(null);
        return TrustScoreResponse.builder()
                .userId(score.getUser().getId())
                .playerName(score.getUser().getName())
                .score(score.getScore())
                .tier(tier(score.getScore()))
                .attendanceScore(score.getAttendanceScore())
                .punctualityScore(score.getPunctualityScore())
                .cancellationScore(score.getCancellationScore())
                .reportScore(score.getReportScore())
                .paymentScore(score.getPaymentScore())
                .matchCompletionScore(score.getMatchCompletionScore())
                .riskLevel(risk == null ? TrustRiskLevel.LOW : risk.getRiskLevel())
                .riskReason(risk == null ? "Player currently has a healthy trust profile." : risk.getReason())
                .badges(badges(score.getScore(), score.getPunctualityScore()))
                .recommendations(recommendations(score.getScore()))
                .updatedAt(score.getUpdatedAt())
                .build();
    }

    private TrustAttendanceResponse mapAttendance(TrustAttendance attendance) {
        int delay = calculateDelayMinutes(attendance.getBooking(), attendance.getCheckinTime());
        String impact = delay < 5 ? "NO_PENALTY" : delay <= 15 ? "SMALL_PENALTY" : "HIGH_PENALTY";
        return TrustAttendanceResponse.builder()
                .id(attendance.getId())
                .bookingId(attendance.getBooking().getId())
                .userId(attendance.getUser().getId())
                .playerName(attendance.getUser().getName())
                .status(attendance.getStatus())
                .delayMinutes(Math.max(delay, 0))
                .impact(impact)
                .checkinTime(attendance.getCheckinTime())
                .createdAt(attendance.getCreatedAt())
                .build();
    }

    private TrustReportResponse mapReport(TrustReport report) {
        return TrustReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getName())
                .reportedUserId(report.getReportedUser().getId())
                .reportedUserName(report.getReportedUser().getName())
                .bookingId(report.getBooking() == null ? null : report.getBooking().getId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private int calculateDelayMinutes(Booking booking, LocalDateTime checkinTime) {
        if (booking == null || booking.getSlot() == null || checkinTime == null) {
            return 0;
        }
        LocalDateTime expected = LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getStartTime());
        return (int) Duration.between(expected, checkinTime).toMinutes();
    }

    private int cancellationPenalty(Booking booking) {
        LocalDateTime start = LocalDateTime.of(booking.getBookingDate(), booking.getSlot().getStartTime());
        long hours = Duration.between(LocalDateTime.now(), start).toHours();
        if (hours >= 24) return 2;
        if (hours >= 3) return 8;
        return 18;
    }

    private void validateOwnerOrAdmin(User owner, Booking booking) {
        boolean admin = hasRole(owner, RoleName.ROLE_ADMIN);
        if (!admin && !booking.getTurf().getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("Only the turf owner can update attendance for this booking.");
        }
    }

    private void validateReportAccess(User reporter, Booking booking) {
        boolean admin = hasRole(reporter, RoleName.ROLE_ADMIN);
        boolean owner = booking.getTurf().getOwner().getId().equals(reporter.getId());
        boolean player = booking.getUser().getId().equals(reporter.getId());
        if (!admin && !owner && !player) {
            throw ApiException.forbidden("You can only report players from bookings you are involved in.");
        }
    }

    private boolean hasRole(User user, RoleName role) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == role);
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private String tier(int score) {
        if (score >= 95) return "Elite Reliable";
        if (score >= 80) return "Trusted";
        if (score >= 60) return "Average";
        if (score >= 40) return "Risky";
        return "Restricted";
    }

    private List<String> badges(int score, int punctualityScore) {
        java.util.ArrayList<String> badges = new java.util.ArrayList<>();
        if (score >= 90) badges.add("Trusted Player");
        if (score >= 95) badges.add("Elite Reliable");
        if (punctualityScore >= 90) badges.add("Punctual Player");
        if (badges.isEmpty()) badges.add("Building Trust");
        return badges;
    }

    private List<String> recommendations(int score) {
        if (score < 40) return List.of("Require owner approval", "Collect advance payment", "Manual review recommended");
        if (score < 70) return List.of("Show high-risk warning to owners", "Limit repeated peak-hour bookings");
        if (score >= 90) return List.of("Eligible for priority booking", "Eligible for trust badge rewards");
        return List.of("Keep attendance strong to unlock Trusted Player benefits");
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
