package com.example.Cricbook.service;

import com.example.Cricbook.dto.SecuritySessionsResponse;
import com.example.Cricbook.dto.SessionRequestContext;
import com.example.Cricbook.dto.UserSessionResponse;
import com.example.Cricbook.entity.SecurityEvent;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.UserSession;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.SecurityEventRepository;
import com.example.Cricbook.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
public class SessionManagementService {

    private final UserSessionRepository userSessionRepository;
    private final SecurityEventRepository securityEventRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${session.remember-refresh-expiration:2592000000}")
    private long rememberRefreshExpiration;

    @Value("${session.player.max-devices:5}")
    private int playerMaxDevices;

    @Value("${session.owner.max-devices:3}")
    private int ownerMaxDevices;

    @Value("${session.admin.max-devices:1}")
    private int adminMaxDevices;

    @Value("${session.player.idle-timeout-minutes:30}")
    private long playerIdleTimeoutMinutes;

    @Value("${session.owner.idle-timeout-minutes:20}")
    private long ownerIdleTimeoutMinutes;

    @Value("${session.admin.idle-timeout-minutes:15}")
    private long adminIdleTimeoutMinutes;

    @Value("${session.owner.absolute-timeout-hours:8}")
    private long ownerAbsoluteTimeoutHours;

    @Value("${session.admin.absolute-timeout-hours:4}")
    private long adminAbsoluteTimeoutHours;

    public SessionManagementService(UserSessionRepository userSessionRepository,
                                    SecurityEventRepository securityEventRepository) {
        this.userSessionRepository = userSessionRepository;
        this.securityEventRepository = securityEventRepository;
    }

    @Transactional
    public UserSession createSession(User user, boolean rememberMe, SessionRequestContext context) {
        SessionRequestContext safeContext = safeContext(context);
        SessionPolicy policy = policyFor(user, rememberMe);
        LocalDateTime now = LocalDateTime.now();
        UserSession session = UserSession.builder()
                .sessionId("sess_" + tokenBytes(18))
                .user(user)
                .deviceName(safeContext.getDeviceName())
                .browser(safeContext.getBrowser())
                .os(safeContext.getOs())
                .ipAddress(safeContext.getIpAddress())
                .userAgent(safeContext.getUserAgent())
                .refreshToken(generateRefreshToken())
                .loginTime(now)
                .lastActivity(now)
                .expiresAt(now.plus(policy.refreshDuration()))
                .absoluteExpiresAt(policy.absoluteDuration() == null ? null : now.plus(policy.absoluteDuration()))
                .active(true)
                .rememberMe(rememberMe)
                .build();

        UserSession saved = userSessionRepository.save(session);
        enforceConcurrentSessionLimit(user, saved.getSessionId(), policy.maxDevices());
        recordSecurityEvent(user, "LOGIN", saved.getSessionId(), safeContext, "Session created.");
        return saved;
    }

    @Transactional
    public UserSession rotateRefreshToken(String submittedRefreshToken, SessionRequestContext context) {
        if (submittedRefreshToken == null || submittedRefreshToken.isBlank()) {
            throw ApiException.unauthorized("Refresh token is required.");
        }
        SessionRequestContext safeContext = safeContext(context);
        UserSession session = userSessionRepository.findByRefreshToken(submittedRefreshToken.trim())
                .orElse(null);

        if (session == null) {
            UserSession reusedSession = userSessionRepository.findByPreviousRefreshToken(submittedRefreshToken.trim()).orElse(null);
            if (reusedSession != null) {
                UserSession reused = reusedSession;
                revokeAllSessions(reused.getUser(), "REFRESH_TOKEN_REUSE");
                recordSecurityEvent(reused.getUser(), "REFRESH_TOKEN_REUSE", reused.getSessionId(), safeContext, "A rotated refresh token was reused.");
                throw ApiException.unauthorized("Refresh token reuse detected. Please sign in again.");
            }
            throw ApiException.unauthorized("Refresh token is invalid or has been rotated. Please sign in again.");
        }

        if (session.getUser().getStatus() != UserStatus.ACTIVE) {
            deactivate(session, "ACCOUNT_INACTIVE", safeContext);
            throw ApiException.unauthorized("Account is not active. Please sign in again after verification.");
        }

        if (!isUsable(session, LocalDateTime.now())) {
            deactivate(session, "SESSION_EXPIRED", safeContext);
            throw ApiException.unauthorized("Session expired. Please sign in again.");
        }

        LocalDateTime now = LocalDateTime.now();
        SessionPolicy policy = policyFor(session.getUser(), session.isRememberMe());
        session.setPreviousRefreshToken(session.getRefreshToken());
        session.setPreviousRefreshTokenUsedAt(now);
        session.setRefreshToken(generateRefreshToken());
        session.setLastActivity(now);
        session.setExpiresAt(now.plus(policy.refreshDuration()));
        session.setIpAddress(safeContext.getIpAddress());
        session.setUserAgent(safeContext.getUserAgent());
        recordSecurityEvent(session.getUser(), "TOKEN_REFRESH", session.getSessionId(), safeContext, "Refresh token rotated.");
        return userSessionRepository.save(session);
    }

    @Transactional
    public boolean touchAndValidateSession(String sessionId, String username) {
        if (sessionId == null || sessionId.isBlank()) {
            return true;
        }
        UserSession session = userSessionRepository.findBySessionId(sessionId).orElse(null);
        if (session == null) {
            return false;
        }
        if (!Objects.equals(session.getUser().getEmail(), username)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!isUsable(session, now)) {
            deactivate(session, "SESSION_EXPIRED", SessionRequestContext.system());
            return false;
        }
        session.setLastActivity(now);
        userSessionRepository.save(session);
        return true;
    }

    @Transactional
    public void logoutSessionByRefreshToken(String refreshToken, SessionRequestContext context) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        userSessionRepository.findByRefreshToken(refreshToken.trim())
                .ifPresent(session -> deactivate(session, "LOGOUT", safeContext(context)));
    }

    @Transactional
    public void logoutSessionBySessionId(String sessionId, SessionRequestContext context) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        userSessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> deactivate(session, "LOGOUT", safeContext(context)));
    }

    @Transactional
    public void revokeAllSessions(User user, String reason) {
        SessionRequestContext context = SessionRequestContext.system();
        for (UserSession session : userSessionRepository.findByUserAndActiveTrueOrderByLoginTimeAsc(user)) {
            deactivate(session, reason, context);
        }
    }

    @Transactional(readOnly = true)
    public SecuritySessionsResponse listSessions(User user, String currentSessionId) {
        List<UserSessionResponse> sessions = userSessionRepository.findByUserAndActiveTrueOrderByLastActivityDesc(user).stream()
                .filter(session -> isUsable(session, LocalDateTime.now()))
                .map(session -> toResponse(session, currentSessionId))
                .toList();
        return SecuritySessionsResponse.builder()
                .activeCount(sessions.size())
                .sessions(sessions)
                .build();
    }

    @Transactional
    public void revokeSession(User user, String sessionId, SessionRequestContext context) {
        UserSession session = userSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session was not found."));
        if (!Objects.equals(session.getUser().getId(), user.getId())) {
            throw ApiException.forbidden("You cannot revoke another user's session.");
        }
        deactivate(session, "REVOKED_BY_USER", safeContext(context));
    }

    public int activeSessionCount(User user) {
        return (int) userSessionRepository.findByUserAndActiveTrueOrderByLastActivityDesc(user).stream()
                .filter(session -> isUsable(session, LocalDateTime.now()))
                .count();
    }

    public String latestActivity(User user) {
        return userSessionRepository.findByUserAndActiveTrueOrderByLastActivityDesc(user).stream()
                .filter(session -> isUsable(session, LocalDateTime.now()))
                .findFirst()
                .map(session -> format(session.getLastActivity()))
                .orElse("No active sessions");
    }

    public String currentDevice(User user) {
        return userSessionRepository.findByUserAndActiveTrueOrderByLastActivityDesc(user).stream()
                .filter(session -> isUsable(session, LocalDateTime.now()))
                .findFirst()
                .map(UserSession::getDeviceName)
                .orElse("Current browser");
    }

    private void enforceConcurrentSessionLimit(User user, String currentSessionId, int maxDevices) {
        List<UserSession> activeSessions = userSessionRepository.findByUserAndActiveTrueOrderByLoginTimeAsc(user);
        int overflow = activeSessions.size() - maxDevices;
        if (overflow <= 0) {
            return;
        }
        for (UserSession session : activeSessions) {
            if (overflow <= 0) {
                break;
            }
            if (currentSessionId.equals(session.getSessionId())) {
                continue;
            }
            deactivate(session, "MAX_DEVICE_LIMIT", SessionRequestContext.system());
            overflow--;
        }
    }

    private boolean isUsable(UserSession session, LocalDateTime now) {
        if (!session.isActive()) {
            return false;
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            return false;
        }
        if (session.getAbsoluteExpiresAt() != null && session.getAbsoluteExpiresAt().isBefore(now)) {
            return false;
        }
        Duration idleTimeout = policyFor(session.getUser(), session.isRememberMe()).idleTimeout();
        return session.getLastActivity() == null || !session.getLastActivity().plus(idleTimeout).isBefore(now);
    }

    private void deactivate(UserSession session, String reason, SessionRequestContext context) {
        if (!session.isActive() && session.getLogoutTime() != null) {
            return;
        }
        session.setActive(false);
        session.setLogoutTime(LocalDateTime.now());
        session.setRevokedReason(reason);
        userSessionRepository.save(session);
        recordSecurityEvent(session.getUser(), reason, session.getSessionId(), context, "Session deactivated.");
    }

    private void recordSecurityEvent(User user, String eventType, String sessionId, SessionRequestContext context, String details) {
        SessionRequestContext safeContext = safeContext(context);
        securityEventRepository.save(SecurityEvent.builder()
                .user(user)
                .eventType(eventType)
                .sessionId(sessionId)
                .ipAddress(safeContext.getIpAddress())
                .userAgent(safeContext.getUserAgent())
                .details(details)
                .build());
    }

    private UserSessionResponse toResponse(UserSession session, String currentSessionId) {
        return UserSessionResponse.builder()
                .sessionId(session.getSessionId())
                .deviceName(session.getDeviceName())
                .browser(session.getBrowser())
                .os(session.getOs())
                .ipAddress(session.getIpAddress())
                .loginTime(format(session.getLoginTime()))
                .lastActivity(format(session.getLastActivity()))
                .expiresAt(format(session.getExpiresAt()))
                .current(Objects.equals(session.getSessionId(), currentSessionId))
                .rememberMe(session.isRememberMe())
                .build();
    }

    private SessionPolicy policyFor(User user, boolean rememberMe) {
        boolean admin = hasRole(user, RoleName.ROLE_ADMIN);
        boolean owner = hasRole(user, RoleName.ROLE_OWNER);
        if (admin) {
            return new SessionPolicy(adminMaxDevices, Duration.ofMinutes(adminIdleTimeoutMinutes), Duration.ofMillis(refreshExpiration), Duration.ofHours(adminAbsoluteTimeoutHours));
        }
        if (owner) {
            return new SessionPolicy(ownerMaxDevices, Duration.ofMinutes(ownerIdleTimeoutMinutes), Duration.ofMillis(refreshExpiration), Duration.ofHours(ownerAbsoluteTimeoutHours));
        }
        return new SessionPolicy(playerMaxDevices, Duration.ofMinutes(playerIdleTimeoutMinutes), Duration.ofMillis(rememberMe ? rememberRefreshExpiration : refreshExpiration), null);
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String generateRefreshToken() {
        return "rt_" + tokenBytes(48);
    }

    private String tokenBytes(int byteCount) {
        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private SessionRequestContext safeContext(SessionRequestContext context) {
        return context == null ? SessionRequestContext.system() : context;
    }

    private record SessionPolicy(int maxDevices, Duration idleTimeout, Duration refreshDuration, Duration absoluteDuration) {
    }
}
