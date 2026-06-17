package com.example.Cricbook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_user_sessions_user_active", columnList = "user_id,is_active"),
        @Index(name = "idx_user_sessions_refresh_token", columnList = "refresh_token"),
        @Index(name = "idx_user_sessions_previous_refresh_token", columnList = "previous_refresh_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 120)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(length = 120)
    private String browser;

    @Column(length = 120)
    private String os;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 255)
    private String refreshToken;

    @Column(name = "previous_refresh_token", length = 255)
    private String previousRefreshToken;

    @Column(name = "previous_refresh_token_used_at")
    private LocalDateTime previousRefreshTokenUsedAt;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "absolute_expires_at")
    private LocalDateTime absoluteExpiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "revoked_reason", length = 120)
    private String revokedReason;
}
