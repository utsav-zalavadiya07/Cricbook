package com.example.Cricbook.service;

import com.example.Cricbook.dto.SessionRequestContext;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.UserSession;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.SecurityEventRepository;
import com.example.Cricbook.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionManagementServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private SecurityEventRepository securityEventRepository;

    private SessionManagementService service;

    @BeforeEach
    void setUp() {
        service = new SessionManagementService(userSessionRepository, securityEventRepository);
        ReflectionTestUtils.setField(service, "refreshExpiration", 604800000L);
        ReflectionTestUtils.setField(service, "rememberRefreshExpiration", 2592000000L);
        ReflectionTestUtils.setField(service, "playerMaxDevices", 5);
        ReflectionTestUtils.setField(service, "ownerMaxDevices", 3);
        ReflectionTestUtils.setField(service, "adminMaxDevices", 1);
        ReflectionTestUtils.setField(service, "playerIdleTimeoutMinutes", 30L);
        ReflectionTestUtils.setField(service, "ownerIdleTimeoutMinutes", 20L);
        ReflectionTestUtils.setField(service, "adminIdleTimeoutMinutes", 15L);
        ReflectionTestUtils.setField(service, "ownerAbsoluteTimeoutHours", 8L);
        ReflectionTestUtils.setField(service, "adminAbsoluteTimeoutHours", 4L);
    }

    @Test
    void createSessionUsesRememberMeRefreshWindow() {
        User user = User.builder()
                .id(1L)
                .email("player@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userSessionRepository.findByUserAndActiveTrueOrderByLoginTimeAsc(user)).thenReturn(List.of());

        UserSession session = service.createSession(user, true, SessionRequestContext.system());

        assertTrue(session.isRememberMe());
        assertTrue(session.getRefreshToken().startsWith("rt_"));
        assertTrue(session.getExpiresAt().isAfter(LocalDateTime.now().plusDays(20)));
        verify(securityEventRepository).save(any());
    }

    @Test
    void rotateRefreshTokenDetectsReuseOfPreviousTokenAndRevokesSessions() {
        User user = User.builder()
                .id(1L)
                .email("player@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        UserSession activeSession = UserSession.builder()
                .sessionId("sess-one")
                .user(user)
                .active(true)
                .lastActivity(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .previousRefreshToken("old-token")
                .build();

        when(userSessionRepository.findByRefreshToken("old-token")).thenReturn(Optional.empty());
        when(userSessionRepository.findByPreviousRefreshToken("old-token")).thenReturn(Optional.of(activeSession));
        when(userSessionRepository.findByUserAndActiveTrueOrderByLoginTimeAsc(user)).thenReturn(List.of(activeSession));

        ApiException error = assertThrows(ApiException.class, () -> service.rotateRefreshToken("old-token", SessionRequestContext.system()));

        assertEquals("Refresh token reuse detected. Please sign in again.", error.getMessage());
        assertFalse(activeSession.isActive());
        assertEquals("REFRESH_TOKEN_REUSE", activeSession.getRevokedReason());
        verify(userSessionRepository, atLeastOnce()).save(activeSession);
        verify(securityEventRepository, atLeast(2)).save(any());
    }

    @Test
    void touchAndValidateSessionUpdatesLastActivityForActiveSession() {
        User user = User.builder()
                .id(1L)
                .email("player@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        UserSession session = UserSession.builder()
                .sessionId("sess-one")
                .user(user)
                .active(true)
                .lastActivity(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(userSessionRepository.findBySessionId("sess-one")).thenReturn(Optional.of(session));

        assertTrue(service.touchAndValidateSession("sess-one", "player@example.com"));

        assertTrue(session.getLastActivity().isAfter(LocalDateTime.now().minusMinutes(1)));
        verify(userSessionRepository).save(session);
    }
}
