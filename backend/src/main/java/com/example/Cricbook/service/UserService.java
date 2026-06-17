package com.example.Cricbook.service;

import com.example.Cricbook.dto.ChangePasswordRequest;
import com.example.Cricbook.dto.DeactivateAccountRequest;
import com.example.Cricbook.dto.PlayerAvailabilityRequest;
import com.example.Cricbook.dto.PlayerAvailabilityResponse;
import com.example.Cricbook.dto.PlayerProfileResponse;
import com.example.Cricbook.dto.UpdatePlayerProfileRequest;
import com.example.Cricbook.dto.UserProfileDto;
import com.example.Cricbook.entity.PlayerAvailability;
import com.example.Cricbook.entity.PlayerProfile;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.PlayerAvailabilityRepository;
import com.example.Cricbook.repository.PlayerProfileRepository;
import com.example.Cricbook.repository.RefreshTokenRepository;
import com.example.Cricbook.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final int NAME_MAX_LENGTH = 80;
    private static final int BIO_MAX_LENGTH = 500;
    private static final int SHORT_TEXT_MAX_LENGTH = 80;
    private static final int URL_MAX_LENGTH = 500;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$");

    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final PlayerAvailabilityRepository playerAvailabilityRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionManagementService sessionManagementService;

    public UserService(UserRepository userRepository,
                       PlayerProfileRepository playerProfileRepository,
                       PlayerAvailabilityRepository playerAvailabilityRepository,
                       PasswordEncoder passwordEncoder,
                       RefreshTokenRepository refreshTokenRepository,
                       SessionManagementService sessionManagementService) {
        this.userRepository = userRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.playerAvailabilityRepository = playerAvailabilityRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionManagementService = sessionManagementService;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUser(String loginId) {
        User user = findUser(loginId);
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElse(null);

        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferredLanguage(user.getPreferredLanguage())
                .preferredTheme(user.getPreferredTheme())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()))
                .profileCompletionPercentage(profileCompletion(user, profile))
                .createdAt(user.getCreatedAt())
                .build();
    }

    public String getPreferredLanguage(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .map(user -> sanitizePreferredLanguage(user.getPreferredLanguage()))
                .orElse("en");
    }

    public String updatePreferredLanguage(String loginId, String preferredLanguage) {
        User user = userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        user.setPreferredLanguage(sanitizePreferredLanguage(preferredLanguage));
        userRepository.save(user);
        return user.getPreferredLanguage();
    }

    public String getPreferredTheme(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .map(user -> sanitizePreferredTheme(user.getPreferredTheme()))
                .orElse("dark");
    }

    public String updatePreferredTheme(String loginId, String preferredTheme) {
        User user = userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        user.setPreferredTheme(sanitizePreferredTheme(preferredTheme));
        userRepository.save(user);
        return user.getPreferredTheme();
    }

    @Transactional
    public void updateProfile(String loginId, com.example.Cricbook.dto.UpdateUserRequest request) {
        User user = findUser(loginId);

        if (request == null) {
            throw ApiException.badRequest("Profile details are required.");
        }

        if (request.getName() != null) {
            user.setName(requiredText(request.getName(), "Full name", NAME_MAX_LENGTH));
        }
        if (request.getPhone() != null) {
            user.setPhone(validPhone(request.getPhone()));
        }

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String loginId, ChangePasswordRequest request) {
        User user = findUser(loginId);
        if (request == null) {
            throw ApiException.badRequest("Password change details are required.");
        }
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw ApiException.badRequest("Current password is required.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect.");
        }
        validateNewPassword(request.getNewPassword(), request.getConfirmPassword(), user);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        revokeAccessTokens(user);
        userRepository.save(user);
        sessionManagementService.revokeAllSessions(user, "PASSWORD_CHANGE");
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public void deactivateAccount(String loginId, DeactivateAccountRequest request) {
        User user = findUser(loginId);
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            throw ApiException.badRequest("Password is required to deactivate your account.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw ApiException.badRequest("Password is incorrect.");
        }
        user.setStatus(UserStatus.DEACTIVATED);
        revokeAccessTokens(user);
        userRepository.save(user);
        sessionManagementService.revokeAllSessions(user, "ACCOUNT_DEACTIVATED");
        refreshTokenRepository.deleteByUser(user);
    }

    public PlayerProfileResponse getMyPlayerProfile(String loginId) {
        User user = findUser(loginId);
        return mapPlayerProfile(user);
    }

    public PlayerProfileResponse getPlayerProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        return mapPlayerProfile(user);
    }

    public PlayerProfileResponse updatePlayerProfile(String loginId, UpdatePlayerProfileRequest request) {
        User user = findUser(loginId);
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId())
                .orElse(PlayerProfile.builder()
                        .user(user)
                        .matchesPlayed(0)
                        .wins(0)
                        .mvpCount(0)
                        .rating(0.0)
                        .build());

        if (request != null) {
            profile.setPhotoUrl(validPhotoUrl(request.getPhotoUrl()));
            profile.setBio(optionalText(request.getBio(), "Bio", BIO_MAX_LENGTH));
            profile.setCity(optionalText(request.getCity(), "City", SHORT_TEXT_MAX_LENGTH));
            profile.setPreferredTime(optionalText(request.getPreferredTime(), "Preferred time", SHORT_TEXT_MAX_LENGTH));
            profile.setBattingStyle(request.getBattingStyle());
            profile.setBowlingStyle(request.getBowlingStyle());
            profile.setPosition(request.getPosition());
            profile.setExperience(optionalText(request.getExperience(), "Experience", SHORT_TEXT_MAX_LENGTH));
        }

        playerProfileRepository.save(profile);
        return mapPlayerProfile(user);
    }

    public PlayerAvailabilityResponse getMyAvailability(String loginId) {
        User user = userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        return mapAvailability(user);
    }

    public PlayerAvailabilityResponse updateAvailability(String loginId, PlayerAvailabilityRequest request) {
        User user = userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
        PlayerAvailability availability = playerAvailabilityRepository.findByUserId(user.getId())
                .orElse(PlayerAvailability.builder().user(user).canTravel(Boolean.TRUE).build());

        if (request != null) {
            availability.setAvailableDays(request.getAvailableDays());
            availability.setPreferredWindow(request.getPreferredWindow());
            availability.setHomeCity(request.getHomeCity());
            availability.setCanTravel(request.getCanTravel());
            availability.setNotes(request.getNotes());
        }

        playerAvailabilityRepository.save(availability);
        return mapAvailability(user);
    }

    private PlayerProfileResponse mapPlayerProfile(User user) {
        PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElse(null);
        return PlayerProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .photoUrl(profile == null ? null : profile.getPhotoUrl())
                .bio(profile == null ? null : profile.getBio())
                .city(profile == null ? null : profile.getCity())
                .preferredTime(profile == null ? null : profile.getPreferredTime())
                .battingStyle(profile == null ? null : profile.getBattingStyle())
                .bowlingStyle(profile == null ? null : profile.getBowlingStyle())
                .position(profile == null ? null : profile.getPosition())
                .experience(profile == null ? null : profile.getExperience())
                .matches(profile == null ? 0 : profile.getMatchesPlayed())
                .wins(profile == null ? 0 : profile.getWins())
                .mvp(profile == null ? 0 : profile.getMvpCount())
                .rating(profile == null ? 0.0 : profile.getRating())
                .profileCompletionPercentage(profileCompletion(user, profile))
                .build();
    }

    private PlayerAvailabilityResponse mapAvailability(User user) {
        PlayerAvailability availability = playerAvailabilityRepository.findByUserId(user.getId()).orElse(null);
        return PlayerAvailabilityResponse.builder()
                .id(availability == null ? null : availability.getId())
                .userId(user.getId())
                .userName(user.getName())
                .availableDays(availability == null ? null : availability.getAvailableDays())
                .preferredWindow(availability == null ? null : availability.getPreferredWindow())
                .homeCity(availability == null ? null : availability.getHomeCity())
                .canTravel(availability == null ? null : availability.getCanTravel())
                .notes(availability == null ? null : availability.getNotes())
                .updatedAt(availability == null ? null : availability.getUpdatedAt())
                .build();
    }

    private String sanitizePreferredLanguage(String preferredLanguage) {
        if (preferredLanguage == null || preferredLanguage.trim().isEmpty()) {
            return "en";
        }

        return preferredLanguage.trim().toLowerCase();
    }

    private String sanitizePreferredTheme(String preferredTheme) {
        if (preferredTheme == null || preferredTheme.trim().isEmpty()) {
            return "dark";
        }

        String normalized = preferredTheme.trim().toLowerCase();
        if (!normalized.equals("light") && !normalized.equals("dark") && !normalized.equals("system")) {
            throw ApiException.badRequest("Theme must be light, dark, or system.");
        }
        return normalized;
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private String requiredText(String value, String field, int maxLength) {
        String normalized = normalizeText(value, maxLength, field);
        if (normalized.isBlank()) {
            throw ApiException.badRequest(field + " is required.");
        }
        return normalized;
    }

    private String optionalText(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeText(value, maxLength, field);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeText(String value, int maxLength, String field) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        normalized = normalized.replace("<", "&lt;").replace(">", "&gt;");
        if (normalized.length() > maxLength) {
            throw ApiException.badRequest(field + " must be " + maxLength + " characters or fewer.");
        }
        return normalized;
    }

    private String validPhone(String phone) {
        String normalized = phone == null ? "" : phone.trim().replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            throw ApiException.badRequest("Phone number is required.");
        }
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw ApiException.badRequest("Phone number must contain 7 to 15 digits and may start with +.");
        }
        return normalized;
    }

    private String validPhotoUrl(String photoUrl) {
        if (photoUrl == null) {
            return null;
        }
        String normalized = normalizeText(photoUrl, URL_MAX_LENGTH, "Photo URL");
        if (normalized.isBlank()) {
            return null;
        }
        if (!(normalized.startsWith("/uploads/") || normalized.startsWith("https://") || normalized.startsWith("http://"))) {
            throw ApiException.badRequest("Photo URL must be an uploaded Cricbook image or a valid HTTP URL.");
        }
        return normalized;
    }

    private void validateNewPassword(String newPassword, String confirmPassword, User user) {
        if (newPassword == null || newPassword.isBlank()) {
            throw ApiException.badRequest("New password is required.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw ApiException.badRequest("Confirm password is required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw ApiException.badRequest("New password and confirm password must match.");
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw ApiException.badRequest("Password must be 8 to 72 characters and include uppercase, lowercase, number, and special character.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw ApiException.badRequest("New password must be different from your current password.");
        }
    }

    private void revokeAccessTokens(User user) {
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
    }

    private int profileCompletion(User user, PlayerProfile profile) {
        int score = 0;
        if (hasValue(profile == null ? null : profile.getPhotoUrl())) score += 20;
        if (hasValue(profile == null ? null : profile.getBio())) score += 20;
        if (hasValue(profile == null ? null : profile.getCity())) score += 20;
        if (hasValue(user.getEmail())) score += 20;
        if (hasValue(user.getPhone())) score += 20;
        return score;
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
