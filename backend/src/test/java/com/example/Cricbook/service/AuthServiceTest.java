package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.security.AdminUserDetails;
import com.example.Cricbook.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;
    @Mock
    private SessionManagementService sessionManagementService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(sessionManagementService.createSession(any(User.class), anyBoolean(), any(SessionRequestContext.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    boolean rememberMe = invocation.getArgument(1);
                    return UserSession.builder()
                            .sessionId("sess-test")
                            .user(user)
                            .refreshToken("refresh-token-rotated")
                            .expiresAt(LocalDateTime.now().plusDays(7))
                            .active(true)
                            .rememberMe(rememberMe)
                            .build();
                });
        lenient().when(sessionManagementService.rotateRefreshToken(anyString(), any(SessionRequestContext.class)))
                .thenThrow(ApiException.unauthorized("Refresh token is invalid or has been rotated. Please sign in again."));
        lenient().when(jwtUtil.generateToken(anyMap(), any())).thenReturn("token");
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");
        request.setPassword("Password123!");

        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        
        Role role = new Role(1L, RoleName.ROLE_USER);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .email(request.getEmail())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        String result = authService.register(request);

        assertEquals("User registered successfully. Please verify your OTP sent to email.", result);
        verify(emailService).sendOtp(eq(request.getEmail()), anyString());
        verify(otpRepository).save(any(Otp.class));
    }

    @Test
    void testRegister_NormalizesEmailAndStoresEncodedPasswordOnly() {
        RegisterRequest request = new RegisterRequest();
        request.setName("  Jane   Player  ");
        request.setEmail("Jane.Player@Example.COM ");
        request.setPhone("9876543210");
        request.setPassword("Password1!");

        when(userRepository.existsByEmailIgnoreCase("jane.player@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("9876543210")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(new Role(1L, RoleName.ROLE_USER)));
        when(passwordEncoder.encode("Password1!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });

        authService.register(request);

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User saved = userCaptor.getAllValues().get(0);
        assertEquals("Jane Player", saved.getName());
        assertEquals("jane.player@example.com", saved.getEmail());
        assertEquals("bcrypt-hash", saved.getPassword());
        assertNotEquals("Password1!", saved.getPassword());
        assertEquals(UserStatus.UNVERIFIED, saved.getStatus());
        assertEquals(0, saved.getTokenVersion());
    }

    @Test
    void testRegister_OwnerRoleAssigned() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Owner User");
        request.setEmail("owner@example.com");
        request.setPhone("7777777777");
        request.setPassword("Password1!");
        request.setRole("OWNER");

        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("7777777777")).thenReturn(false);
        Role role = new Role(2L, RoleName.ROLE_OWNER);
        when(roleRepository.findByName(RoleName.ROLE_OWNER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        authService.register(request);

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        assertTrue(userCaptor.getAllValues().get(0).getRoles().contains(role));
    }

    @Test
    void testRegister_EmailTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("taken@example.com");

        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(request));
    }

    @Test
    void testRegister_PhoneTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPhone("1234567890");

        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(request));
    }

    @Test
    void testVerifyOtp_UserNotFound() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("notfound@example.com");

        when(userRepository.findByEmailIgnoreCase(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> authService.verifyOtp(request));
    }

    @Test
    void testVerifyOtp_AlreadyUsedOtpRejected() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.UNVERIFIED).build();
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        Otp otp = Otp.builder()
                .otpCode("123456")
                .expiry(LocalDateTime.now().plusMinutes(5))
                .verified(true)
                .build();
        when(otpRepository.findTopByUserIdOrderByIdDesc(1L)).thenReturn(Optional.of(otp));

        ApiException error = assertThrows(ApiException.class, () -> authService.verifyOtp(request));

        assertEquals("OTP already used", error.getMessage());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void testVerifyOtp_AlreadyVerified() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");

        User user = User.builder().status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmailIgnoreCase(request.getEmail())).thenReturn(Optional.of(user));

        assertThrows(ApiException.class, () -> authService.verifyOtp(request));
    }

    @Test
    void testVerifyOtp_Success() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.UNVERIFIED).build();
        when(userRepository.findByEmailIgnoreCase(request.getEmail())).thenReturn(Optional.of(user));

        Otp otp = Otp.builder()
                .otpCode("123456")
                .expiry(LocalDateTime.now().plusMinutes(5))
                .build();
        when(otpRepository.findTopByUserIdOrderByIdDesc(1L)).thenReturn(Optional.of(otp));
        AuthResponse response = authService.verifyOtp(request);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(otp.isVerified());
        verify(userRepository).save(user);
    }

    @Test
    void testVerifyOtp_OtpExpired() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        User user = User.builder().id(1L).status(UserStatus.UNVERIFIED).build();
        when(userRepository.findByEmailIgnoreCase(request.getEmail())).thenReturn(Optional.of(user));

        Otp otp = Otp.builder().otpCode("123456").expiry(LocalDateTime.now().minusMinutes(1)).build();
        when(otpRepository.findTopByUserIdOrderByIdDesc(1L)).thenReturn(Optional.of(otp));

        assertThrows(ApiException.class, () -> authService.verifyOtp(request));
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("john@example.com");
        request.setPassword("password");
        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.ACTIVE).build();
        
        Authentication auth = mock(Authentication.class);
        com.example.Cricbook.security.CustomUserDetails userDetails = mock(com.example.Cricbook.security.CustomUserDetails.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token", response.getAccessToken());
        assertEquals("sess-test", response.getSessionId());
    }

    @Test
    void testLogin_RememberMeCreatesRememberedSession() {
        LoginRequest request = new LoginRequest();
        request.setUsername("john@example.com");
        request.setPassword("password");
        request.setRememberMe(true);
        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.ACTIVE).build();

        Authentication auth = mock(Authentication.class);
        com.example.Cricbook.security.CustomUserDetails userDetails = mock(com.example.Cricbook.security.CustomUserDetails.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);

        AuthResponse response = authService.login(request);

        assertEquals("sess-test", response.getSessionId());
        verify(sessionManagementService).createSession(eq(user), eq(true), any(SessionRequestContext.class));
    }

    @Test
    void testLogin_InactiveAccountRejectedAfterAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setUsername("john@example.com");
        request.setPassword("password");
        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.BLOCKED).build();

        Authentication auth = mock(Authentication.class);
        com.example.Cricbook.security.CustomUserDetails userDetails = mock(com.example.Cricbook.security.CustomUserDetails.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(user);

        assertThrows(ApiException.class, () -> authService.login(request));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void testLogin_AdminPrincipalReturnsFriendlyApiException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@cricbook.com");
        request.setPassword("admin123");

        Authentication auth = mock(Authentication.class);
        AdminUser admin = AdminUser.builder()
                .id(1L)
                .email("admin@cricbook.com")
                .password("encoded")
                .role("SUPER_ADMIN")
                .status("ACTIVE")
                .build();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(new AdminUserDetails(admin));

        ApiException error = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals("Use the admin login page for administrator accounts.", error.getMessage());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void testRefreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.ACTIVE).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("refresh-token")
                .expiry(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("new-token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-token", response.getAccessToken());
        verify(refreshTokenRepository).delete(refreshToken);
        verify(sessionManagementService).createSession(eq(user), eq(false), any(SessionRequestContext.class));
    }

    @Test
    void testRefreshToken_RotatesManagedSession() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.ACTIVE).build();
        UserSession rotatedSession = UserSession.builder()
                .sessionId("sess-managed")
                .user(user)
                .refreshToken("refresh-token-next")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .active(true)
                .build();
        when(sessionManagementService.rotateRefreshToken(eq("refresh-token"), any(SessionRequestContext.class))).thenReturn(rotatedSession);
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("managed-access-token");

        AuthResponse response = authService.refreshToken(request);

        assertEquals("managed-access-token", response.getAccessToken());
        assertEquals("refresh-token-next", response.getRefreshToken());
        assertEquals("sess-managed", response.getSessionId());
        verify(refreshTokenRepository, never()).findByToken(anyString());
    }

    @Test
    void testRefreshToken_InactiveUserDeletesTokenAndRejects() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        User user = User.builder().id(1L).email("john@example.com").status(UserStatus.BLOCKED).build();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token("refresh-token")
                .expiry(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        ApiException error = assertThrows(ApiException.class, () -> authService.refreshToken(request));

        assertEquals("Account is not active. Please sign in again after verification.", error.getMessage());
        verify(refreshTokenRepository).delete(refreshToken);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void testResetPasswordRevokesRefreshTokensAndAccessTokenVersion() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("NewPassword1!");
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .password("old-hash")
                .status(UserStatus.ACTIVE)
                .tokenVersion(3)
                .build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash("hash")
                .expiry(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(anyString())).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("NewPassword1!", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-hash");

        String message = authService.resetPassword(request);

        assertEquals("Password reset successfully. Please sign in with your new password.", message);
        assertEquals("new-hash", user.getPassword());
        assertEquals(4, user.getTokenVersion());
        assertTrue(resetToken.isUsed());
        verify(sessionManagementService).revokeAllSessions(user, "PASSWORD_RESET");
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void testLogout() {
        authService.logout("Bearer token", "refresh-token");
        verify(refreshTokenRepository).findByToken("refresh-token");
    }

    @Test
    void testLogoutRevokesAccessTokenVersionAndRefreshTokensForBearerUser() {
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .tokenVersion(1)
                .build();
        when(jwtUtil.extractUsername("access-token")).thenReturn("john@example.com");
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));

        authService.logout("Bearer access-token", null);

        assertEquals(2, user.getTokenVersion());
        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).save(user);
    }

    @Test
    void testLogoutAllRevokesSessionsTokensAndAccessTokenVersion() {
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .tokenVersion(5)
                .build();
        when(jwtUtil.extractUsername("access-token")).thenReturn("john@example.com");
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));

        authService.logoutAll("Bearer access-token", SessionRequestContext.system());

        assertEquals(6, user.getTokenVersion());
        verify(sessionManagementService).revokeAllSessions(user, "LOGOUT_ALL");
        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).save(user);
    }
}
