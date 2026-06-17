package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.security.CustomUserDetails;
import com.example.Cricbook.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpRepository otpRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final SessionManagementService sessionManagementService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository, OtpRepository otpRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       EmailService emailService,
                       SessionManagementService sessionManagementService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpRepository = otpRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.sessionManagementService = sessionManagementService;
    }

    public String generateOtp() {
        return String.valueOf(secureRandom.nextInt(900000) + 100000);
    }

    @Transactional
    public String register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        String name = normalizeName(request.getName());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException("Email is already registered.");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("Phone number is already registered.");
        }

        final RoleName finalRoleName = resolveRequestedRole(request.getRole());

        Role userRole = roleRepository.findByName(finalRoleName)
                .orElseGet(() -> roleRepository.save(new Role(null, finalRoleName)));

        User user = User.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.UNVERIFIED)
                .tokenVersion(0)
                .build();
        user.getRoles().add(userRole);

        user = userRepository.save(user);

        String otp = generateOtp();

        Otp otpEntity = Otp.builder()
                .user(user)
                .otpCode(otp)
                .expiry(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(otpEntity);

        emailService.sendOtp(user.getEmail(), otp);

        return "User registered successfully. Please verify your OTP sent to email.";
    }

    @Transactional
    public String resendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ApiException("User not found with email: " + normalizedEmail));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ApiException("User is already verified and active.");
        }

        String otp = generateOtp();

        otpRepository.deleteByUser(user);

        Otp otpEntity = Otp.builder()
                .user(user)
                .otpCode(otp)
                .expiry(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(otpEntity);

        emailService.sendOtp(user.getEmail(), otp);

        return "New OTP sent successfully to " + normalizedEmail;
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ApiException("User not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ApiException("User is already verified");
        }

        Otp otpEntity = otpRepository.findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow(() -> new ApiException("OTP not found"));

        if (otpEntity.isVerified()) {
            throw new ApiException("OTP already used");
        }

        if (otpEntity.getExpiry().isBefore(LocalDateTime.now())) {
            throw new ApiException("OTP expired");
        }

        if (!otpEntity.getOtpCode().equals(request.getOtp())) {
            throw new ApiException("Invalid OTP");
        }

        otpEntity.setVerified(true);
        otpRepository.save(otpEntity);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        return generateTokens(user, false, SessionRequestContext.system());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, SessionRequestContext.system());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, SessionRequestContext context) {
        String loginId = normalizeLoginId(request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginId, request.getPassword())
        );

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw ApiException.badRequest("Use the admin login page for administrator accounts.");
        }
        User user = userDetails.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException("Account is not active. Status: " + user.getStatus());
        }

        return generateTokens(user, request.isRememberMe(), context);
    }

    @Transactional
    public String requestPasswordReset(ForgotPasswordRequest request) {
        String loginId = normalizeLoginId(request.getLoginId());
        userRepository.findByEmailOrPhone(loginId).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);

            String rawToken = generateSecureToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hashToken(rawToken))
                    .expiry(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl.replaceAll("/+$", "") + "/auth/reset-password?token=" + rawToken;
            emailService.sendPasswordResetLink(user.getEmail(), resetLink);
        });

        return "If an account exists for this email or phone number, a password reset link has been sent.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hashToken(request.getToken().trim()))
                .orElseThrow(() -> ApiException.badRequest("This password reset link is invalid or has already been used."));

        if (resetToken.getExpiry().isBefore(LocalDateTime.now())) {
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);
            throw ApiException.badRequest("This password reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw ApiException.badRequest("New password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        revokeAccessTokens(user);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        sessionManagementService.revokeAllSessions(user, "PASSWORD_RESET");
        refreshTokenRepository.deleteByUser(user);

        return "Password reset successfully. Please sign in with your new password.";
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshToken(request, SessionRequestContext.system());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, SessionRequestContext context) {
        try {
            UserSession session = sessionManagementService.rotateRefreshToken(request.getRefreshToken(), context);
            return buildAuthResponse(session.getUser(), session);
        } catch (ApiException sessionError) {
            if ("Refresh token reuse detected. Please sign in again.".equals(sessionError.getMessage())) {
                throw sessionError;
            }
            RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                    .orElseThrow(() -> sessionError);

            if (refreshToken.getExpiry().isBefore(LocalDateTime.now())) {
                refreshTokenRepository.delete(refreshToken);
                throw new ApiException("Refresh token was expired. Please make a new signin request");
            }

            User user = refreshToken.getUser();
            if (user.getStatus() != UserStatus.ACTIVE) {
                refreshTokenRepository.delete(refreshToken);
                throw ApiException.unauthorized("Account is not active. Please sign in again after verification.");
            }
            refreshTokenRepository.delete(refreshToken);
            UserSession migratedSession = sessionManagementService.createSession(user, false, context);
            return buildAuthResponse(user, migratedSession);
        }
    }

    @Transactional
    public void logout(String authHeader, String refreshTokenStr) {
        logout(authHeader, refreshTokenStr, SessionRequestContext.system());
    }

    @Transactional
    public void logout(String authHeader, String refreshTokenStr, SessionRequestContext context) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            sessionManagementService.logoutSessionByRefreshToken(refreshTokenStr.trim(), context);
            refreshTokenRepository.findByToken(refreshTokenStr.trim()).ifPresent(refreshTokenRepository::delete);
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String accessToken = authHeader.substring(7);
                String sessionId = jwtUtil.extractSessionId(accessToken);
                if (sessionId != null && !sessionId.isBlank()) {
                    sessionManagementService.logoutSessionBySessionId(sessionId, context);
                    return;
                }
                String username = jwtUtil.extractUsername(accessToken);
                userRepository.findByEmailIgnoreCase(username).ifPresent(user -> {
                    revokeAccessTokens(user);
                    sessionManagementService.revokeAllSessions(user, "LEGACY_LOGOUT");
                    refreshTokenRepository.deleteByUser(user);
                    userRepository.save(user);
                });
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Transactional
    public void logoutAll(String authHeader, SessionRequestContext context) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw ApiException.unauthorized("Access token is required.");
        }
        String username = jwtUtil.extractUsername(authHeader.substring(7));
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> ApiException.unauthorized("User session was not found."));
        revokeAccessTokens(user);
        sessionManagementService.revokeAllSessions(user, "LOGOUT_ALL");
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);
    }

    private AuthResponse generateTokens(User user) {
        return generateTokens(user, false, SessionRequestContext.system());
    }

    private AuthResponse generateTokens(User user, boolean rememberMe, SessionRequestContext context) {
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0);
            userRepository.save(user);
        }

        UserSession session = sessionManagementService.createSession(user, rememberMe, context);
        return buildAuthResponse(user, session);
    }

    private AuthResponse buildAuthResponse(User user, UserSession session) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtUtil.generateToken(Map.of("sessionId", session.getSessionId()), userDetails);

        String primaryRole = resolvePrimaryRole(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(session.getRefreshToken())
                .expiresIn(jwtExpiration / 1000)
                .refreshExpiresIn(Math.max(0, Duration.between(LocalDateTime.now(), session.getExpiresAt()).toSeconds()))
                .role(primaryRole)
                .sessionId(session.getSessionId())
                .build();
    }

    private void revokeAccessTokens(User user) {
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
    }

    private RoleName resolveRequestedRole(String role) {
        if (role == null || role.isBlank() || "USER".equalsIgnoreCase(role.trim())) {
            return RoleName.ROLE_USER;
        }
        if ("OWNER".equalsIgnoreCase(role.trim())) {
            return RoleName.ROLE_OWNER;
        }
        throw ApiException.badRequest("Role must be USER or OWNER.");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLoginId(String loginId) {
        String value = loginId == null ? "" : loginId.trim();
        return value.contains("@") ? value.toLowerCase() : value;
    }

    private String resolvePrimaryRole(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);
        if (isAdmin) {
            return "ADMIN";
        }

        boolean isOwner = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_OWNER);
        if (isOwner) {
            return "OWNER";
        }

        return "USER";
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteExpiredOtp() {
        otpRepository.deleteByExpiryBefore(LocalDateTime.now());
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void deleteExpiredPasswordResetTokens() {
        passwordResetTokenRepository.deleteByExpiryBefore(LocalDateTime.now());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw ApiException.internal("Unable to process password reset token.", e);
        }
    }
}
