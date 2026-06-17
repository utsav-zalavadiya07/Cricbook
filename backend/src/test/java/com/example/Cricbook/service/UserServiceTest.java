package com.example.Cricbook.service;

import com.example.Cricbook.dto.ChangePasswordRequest;
import com.example.Cricbook.dto.DeactivateAccountRequest;
import com.example.Cricbook.dto.PlayerProfileResponse;
import com.example.Cricbook.dto.UpdatePlayerProfileRequest;
import com.example.Cricbook.dto.UpdateUserRequest;
import com.example.Cricbook.entity.PlayerProfile;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.PlayerAvailabilityRepository;
import com.example.Cricbook.repository.PlayerProfileRepository;
import com.example.Cricbook.repository.RefreshTokenRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PlayerProfileRepository playerProfileRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private SessionManagementService sessionManagementService;
    private UserService userService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        playerProfileRepository = mock(PlayerProfileRepository.class);
        PlayerAvailabilityRepository playerAvailabilityRepository = mock(PlayerAvailabilityRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sessionManagementService = mock(SessionManagementService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, playerProfileRepository, playerAvailabilityRepository, passwordEncoder, refreshTokenRepository, sessionManagementService);
    }

    @Test
    void updateProfileRejectsBlankNameAndInvalidPhone() {
        User user = user();
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("   ");

        assertThatThrownBy(() -> userService.updateProfile(user.getEmail(), request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Full name is required");

        request.setName("Valid Name");
        request.setPhone("abc123");
        assertThatThrownBy(() -> userService.updateProfile(user.getEmail(), request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Phone number must contain");
    }

    @Test
    void updatePlayerProfileSanitizesTextAndCalculatesCompletion() {
        User user = user();
        PlayerProfile profile = PlayerProfile.builder()
                .user(user)
                .matchesPlayed(0)
                .wins(0)
                .mvpCount(0)
                .rating(0.0)
                .build();
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        when(playerProfileRepository.findByUserId(user.getId())).thenReturn(Optional.of(profile));
        when(playerProfileRepository.save(any(PlayerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdatePlayerProfileRequest request = new UpdatePlayerProfileRequest();
        request.setPhotoUrl("/uploads/users/1/profile/avatar.png");
        request.setBio(" <script>alert('x')</script>  Opening batter ");
        request.setCity("  Surat  ");

        PlayerProfileResponse response = userService.updatePlayerProfile(user.getEmail(), request);

        assertThat(profile.getBio()).contains("&lt;script&gt;");
        assertThat(profile.getCity()).isEqualTo("Surat");
        assertThat(response.getProfileCompletionPercentage()).isEqualTo(100);
    }

    @Test
    void changePasswordUpdatesHashAndRevokesTokens() {
        User user = user();
        user.setPassword(passwordEncoder.encode("OldPass1!"));
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass1!");
        request.setNewPassword("NewPass1!");
        request.setConfirmPassword("NewPass1!");

        userService.changePassword(user.getEmail(), request);

        assertThat(passwordEncoder.matches("NewPass1!", user.getPassword())).isTrue();
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(sessionManagementService).revokeAllSessions(user, "PASSWORD_CHANGE");
        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).save(user);
    }

    @Test
    void deactivateAccountBlocksFutureLoginByStatusAndRevokesTokens() {
        User user = user();
        user.setPassword(passwordEncoder.encode("OldPass1!"));
        when(userRepository.findByEmailOrPhone(user.getEmail())).thenReturn(Optional.of(user));
        DeactivateAccountRequest request = new DeactivateAccountRequest();
        request.setPassword("OldPass1!");

        userService.deactivateAccount(user.getEmail(), request);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DEACTIVATED);
        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(sessionManagementService).revokeAllSessions(user, "ACCOUNT_DEACTIVATED");
        verify(refreshTokenRepository).deleteByUser(user);
        verify(userRepository).save(user);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .name("Player One")
                .email("player@example.com")
                .phone("9876543210")
                .password("encoded")
                .preferredLanguage("en")
                .preferredTheme("dark")
                .status(UserStatus.ACTIVE)
                .tokenVersion(0)
                .build();
    }
}
