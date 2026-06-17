package com.example.Cricbook.service;

import com.example.Cricbook.dto.MediaAssetResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.enums.TeamVisibility;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MediaServiceTest {

    private UserRepository userRepository;
    private PlayerProfileRepository playerProfileRepository;
    private OwnerSettingsRepository ownerSettingsRepository;
    private TeamRepository teamRepository;
    private MediaAssetRepository mediaAssetRepository;
    private MediaService mediaService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        playerProfileRepository = mock(PlayerProfileRepository.class);
        ownerSettingsRepository = mock(OwnerSettingsRepository.class);
        teamRepository = mock(TeamRepository.class);
        mediaAssetRepository = mock(MediaAssetRepository.class);
        mediaService = new MediaService(userRepository, playerProfileRepository, ownerSettingsRepository, teamRepository, mediaAssetRepository);
        System.setProperty("user.dir", tempDir.toString());
    }

    @Test
    void uploadsProfilePhotoAndSyncsPlayerProfile() {
        User user = user(1L, RoleName.ROLE_USER);
        PlayerProfile profile = PlayerProfile.builder().user(user).matchesPlayed(0).wins(0).mvpCount(0).rating(0.0).build();
        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(playerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(mediaAssetRepository.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> {
            MediaAsset asset = invocation.getArgument(0);
            asset.setId(10L);
            return asset;
        });

        MediaAssetResponse response = mediaService.uploadProfilePhoto("player@example.com", image("avatar.webp", "image/webp"), "TEAMS_ONLY");

        assertThat(response.getImageUrl()).startsWith("/uploads/users/1/profile/");
        assertThat(response.getVisibility()).isEqualTo("TEAMS_ONLY");
        assertThat(profile.getPhotoUrl()).isEqualTo(response.getImageUrl());
        verify(playerProfileRepository).saveAndFlush(profile);
    }

    @Test
    void uploadsProfilePhotoWhenExistingProfileHasMissingCounters() {
        User user = user(1L, RoleName.ROLE_USER);
        PlayerProfile profile = PlayerProfile.builder().user(user).build();
        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(playerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(mediaAssetRepository.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaAssetResponse response = mediaService.uploadProfilePhoto("player@example.com", image("avatar.png", "image/png"), "PUBLIC");

        assertThat(response.getImageUrl()).startsWith("/uploads/users/1/profile/");
        assertThat(profile.getMatchesPlayed()).isZero();
        assertThat(profile.getWins()).isZero();
        assertThat(profile.getMvpCount()).isZero();
        assertThat(profile.getRating()).isZero();
        verify(playerProfileRepository).saveAndFlush(profile);
    }

    @Test
    void rejectsUnsupportedImageTypes() {
        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user(1L, RoleName.ROLE_USER)));

        assertThatThrownBy(() -> mediaService.uploadProfilePhoto("player@example.com", new MockMultipartFile("file", "bad.gif", "image/gif", new byte[]{1}), "PUBLIC"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only JPG, PNG, and WEBP");
    }

    @Test
    void rejectsCorruptedImagePayloadsEvenWhenMimeTypeLooksValid() {
        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user(1L, RoleName.ROLE_USER)));

        assertThatThrownBy(() -> mediaService.uploadProfilePhoto("player@example.com", new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3, 4}), "PUBLIC"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("valid image");
    }

    @Test
    void uploadsOwnerLogoOnlyForOwners() {
        User owner = user(2L, RoleName.ROLE_OWNER);
        OwnerSettings settings = OwnerSettings.builder().owner(owner).businessName("Arena").build();
        when(userRepository.findByEmailOrPhone("owner@example.com")).thenReturn(Optional.of(owner));
        when(ownerSettingsRepository.findByOwnerId(2L)).thenReturn(Optional.of(settings));
        when(mediaAssetRepository.saveAndFlush(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MediaAssetResponse response = mediaService.uploadOwnerLogo("owner@example.com", image("logo.png", "image/png"));

        assertThat(response.getOwnerType()).isEqualTo("OWNER");
        assertThat(settings.getBusinessLogoUrl()).isEqualTo(response.getImageUrl());
        verify(ownerSettingsRepository).save(settings);
    }

    @Test
    void blocksTeamLogoUploadForNonCaptains() {
        User captain = user(3L, RoleName.ROLE_USER);
        User other = user(4L, RoleName.ROLE_USER);
        Team team = Team.builder().id(99L).captain(captain).name("Storm").sport("Cricket").city("Surat").visibility(TeamVisibility.PUBLIC).maxMembers(11).build();
        when(userRepository.findByEmailOrPhone("other@example.com")).thenReturn(Optional.of(other));
        when(teamRepository.findById(99L)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> mediaService.uploadTeamLogo("other@example.com", 99L, image("logo.jpg", "image/jpeg")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the team captain");
    }

    private MockMultipartFile image(String name, String contentType) {
        byte[] bytes = switch (contentType) {
            case "image/png" -> new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
            case "image/webp" -> new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};
            default -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0};
        };
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private User user(Long id, RoleName roleName) {
        return User.builder()
                .id(id)
                .name("Test User")
                .email(id == 2L ? "owner@example.com" : id == 4L ? "other@example.com" : "player@example.com")
                .phone("999999999" + id)
                .password("secret")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().name(roleName).build()))
                .build();
    }
}
