package com.example.Cricbook.service;

import com.example.Cricbook.dto.MediaAssetResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> VISIBILITY = List.of("PUBLIC", "FRIENDS", "TEAMS_ONLY", "PRIVATE");

    private final UserRepository userRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final OwnerSettingsRepository ownerSettingsRepository;
    private final TeamRepository teamRepository;
    private final MediaAssetRepository mediaAssetRepository;

    @Transactional(readOnly = true)
    public MediaAssetResponse getMyProfileMedia(String loginId) {
        User user = findUser(loginId);
        return mediaAssetRepository.findFirstByOwnerTypeAndOwnerIdAndMediaTypeAndActiveTrueOrderByUploadedAtDesc("USER", user.getId(), "PROFILE_PHOTO")
                .map(this::map)
                .orElseGet(() -> fallback("USER", user.getId(), "PROFILE_PHOTO", avatarUrl(user.getName())));
    }

    @Transactional
    public MediaAssetResponse uploadProfilePhoto(String loginId, MultipartFile file, String visibility) {
        User user = findUser(loginId);
        try {
            MediaAsset asset = saveMedia("USER", user.getId(), "PROFILE_PHOTO", "users/" + user.getId() + "/profile", file, visibility);
            PlayerProfile profile = playerProfileRepository.findByUserId(user.getId()).orElseGet(() -> defaultPlayerProfile(user));
            ensureProfileDefaults(profile);
            profile.setPhotoUrl(asset.getImageUrl());
            playerProfileRepository.saveAndFlush(profile);
            return map(asset);
        } catch (DataAccessException ex) {
            throw ApiException.internal("Profile photo could not be saved. Please refresh and try again.", ex);
        }
    }

    @Transactional
    public MediaAssetResponse removeProfilePhoto(String loginId) {
        User user = findUser(loginId);
        deactivate("USER", user.getId(), "PROFILE_PHOTO");
        playerProfileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            profile.setPhotoUrl(null);
            playerProfileRepository.save(profile);
        });
        return fallback("USER", user.getId(), "PROFILE_PHOTO", avatarUrl(user.getName()));
    }

    @Transactional
    public MediaAssetResponse uploadOwnerLogo(String loginId, MultipartFile file) {
        User owner = requireOwner(loginId);
        MediaAsset asset = saveMedia("OWNER", owner.getId(), "BUSINESS_LOGO", "owners/" + owner.getId() + "/logo", file, "PUBLIC");
        OwnerSettings settings = ownerSettingsRepository.findByOwnerId(owner.getId()).orElseGet(() -> OwnerSettings.builder()
                .owner(owner)
                .businessName(owner.getName())
                .supportPhone(owner.getPhone())
                .supportEmail(owner.getEmail())
                .build());
        settings.setBusinessLogoUrl(asset.getImageUrl());
        ownerSettingsRepository.save(settings);
        return map(asset);
    }

    @Transactional
    public MediaAssetResponse removeOwnerLogo(String loginId) {
        User owner = requireOwner(loginId);
        deactivate("OWNER", owner.getId(), "BUSINESS_LOGO");
        ownerSettingsRepository.findByOwnerId(owner.getId()).ifPresent(settings -> {
            settings.setBusinessLogoUrl(null);
            ownerSettingsRepository.save(settings);
        });
        return fallback("OWNER", owner.getId(), "BUSINESS_LOGO", avatarUrl(owner.getName()));
    }

    @Transactional
    public MediaAssetResponse uploadTeamLogo(String loginId, Long teamId, MultipartFile file) {
        User user = findUser(loginId);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> ApiException.notFound("Team was not found."));
        if (!team.getCaptain().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the team captain can update team media.");
        }
        MediaAsset asset = saveMedia("TEAM", team.getId(), "TEAM_LOGO", "teams/" + team.getId() + "/logo", file, "PUBLIC");
        team.setLogoUrl(asset.getImageUrl());
        teamRepository.save(team);
        return map(asset);
    }

    @Transactional
    public MediaAssetResponse removeTeamLogo(String loginId, Long teamId) {
        User user = findUser(loginId);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> ApiException.notFound("Team was not found."));
        if (!team.getCaptain().getId().equals(user.getId())) {
            throw ApiException.forbidden("Only the team captain can update team media.");
        }
        deactivate("TEAM", teamId, "TEAM_LOGO");
        team.setLogoUrl(null);
        teamRepository.save(team);
        return fallback("TEAM", teamId, "TEAM_LOGO", avatarUrl(team.getName()));
    }

    @Transactional(readOnly = true)
    public List<MediaAssetResponse> moderationQueue() {
        return mediaAssetRepository.findByModerationStatusOrderByUploadedAtDesc("PENDING").stream().map(this::map).toList();
    }

    private MediaAsset saveMedia(String ownerType, Long ownerId, String mediaType, String folder, MultipartFile file, String requestedVisibility) {
        validate(file);
        String normalizedVisibility = normalizeVisibility(requestedVisibility);
        deactivate(ownerType, ownerId, mediaType);
        String storedUrl = writeFile(folder, file);
        MediaAsset asset = MediaAsset.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .mediaType(mediaType)
                .imageUrl(storedUrl)
                .thumbnailUrl(storedUrl)
                .visibility(normalizedVisibility)
                .moderationStatus("APPROVED")
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .originalFilename(file.getOriginalFilename())
                .active(true)
                .build();
        return mediaAssetRepository.saveAndFlush(asset);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Please choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("Image must be 5 MB or smaller.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw ApiException.badRequest("Only JPG, PNG, and WEBP images are supported.");
        }
        try {
            byte[] bytes = file.getBytes();
            if (!matchesDeclaredImageType(bytes, contentType.toLowerCase(Locale.ROOT))) {
                throw ApiException.badRequest("Uploaded file does not look like a valid image.");
            }
        } catch (IOException ex) {
            throw ApiException.badRequest("Uploaded image could not be read. Please choose another image.");
        }
    }

    private String writeFile(String folder, MultipartFile file) {
        try {
            String extension = extensionFor(file);
            Path directory = Paths.get("uploads", folder).normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path destination = directory.resolve(filename).normalize();
            Files.write(destination, file.getBytes());
            return "/uploads/" + folder.replace("\\", "/") + "/" + filename;
        } catch (IOException ex) {
            throw ApiException.internal("Unable to upload image right now. Please try again.");
        }
    }

    private String extensionFor(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && original.lastIndexOf('.') >= 0) {
            String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
            if (List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                return ext;
            }
        }
        return switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private void deactivate(String ownerType, Long ownerId, String mediaType) {
        mediaAssetRepository.findFirstByOwnerTypeAndOwnerIdAndMediaTypeAndActiveTrueOrderByUploadedAtDesc(ownerType, ownerId, mediaType)
                .ifPresent(existing -> {
                    existing.setActive(false);
                    mediaAssetRepository.saveAndFlush(existing);
                    deleteLocalUpload(existing.getImageUrl());
                });
    }

    private boolean matchesDeclaredImageType(byte[] bytes, String contentType) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        if ("image/jpeg".equals(contentType)) {
            return bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF
                    && (bytes[1] & 0xFF) == 0xD8
                    && (bytes[2] & 0xFF) == 0xFF;
        }
        if ("image/png".equals(contentType)) {
            byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            if (bytes.length < signature.length) {
                return false;
            }
            for (int i = 0; i < signature.length; i++) {
                if (bytes[i] != signature[i]) {
                    return false;
                }
            }
            return true;
        }
        if ("image/webp".equals(contentType)) {
            return bytes.length >= 12
                    && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                    && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
        }
        return false;
    }

    private void deleteLocalUpload(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            Path path = Paths.get(imageUrl.substring(1)).normalize();
            if (path.startsWith("uploads")) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }

    private String normalizeVisibility(String visibility) {
        String normalized = visibility == null || visibility.isBlank() ? "PUBLIC" : visibility.trim().toUpperCase(Locale.ROOT);
        if (!VISIBILITY.contains(normalized)) {
            throw ApiException.badRequest("Media visibility must be public, friends, teams only, or private.");
        }
        return normalized;
    }

    private User findUser(String loginId) {
        return userRepository.findByEmailOrPhone(loginId)
                .or(() -> userRepository.findByEmail(loginId))
                .or(() -> userRepository.findByPhone(loginId))
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private PlayerProfile defaultPlayerProfile(User user) {
        return PlayerProfile.builder()
                .user(user)
                .matchesPlayed(0)
                .wins(0)
                .mvpCount(0)
                .rating(0.0)
                .build();
    }

    private void ensureProfileDefaults(PlayerProfile profile) {
        if (profile.getMatchesPlayed() == null) profile.setMatchesPlayed(0);
        if (profile.getWins() == null) profile.setWins(0);
        if (profile.getMvpCount() == null) profile.setMvpCount(0);
        if (profile.getRating() == null) profile.setRating(0.0);
    }

    private User requireOwner(String loginId) {
        User user = findUser(loginId);
        boolean allowed = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_OWNER || role.getName() == RoleName.ROLE_ADMIN);
        if (!allowed) {
            throw ApiException.forbidden("Only turf owners can manage business media.");
        }
        return user;
    }

    private MediaAssetResponse fallback(String ownerType, Long ownerId, String mediaType, String avatarUrl) {
        return MediaAssetResponse.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .mediaType(mediaType)
                .imageUrl(avatarUrl)
                .thumbnailUrl(avatarUrl)
                .visibility("PUBLIC")
                .moderationStatus("APPROVED")
                .contentType("image/svg+xml")
                .fileSizeBytes(0L)
                .build();
    }

    private String avatarUrl(String name) {
        String initial = name == null || name.isBlank() ? "C" : name.trim().substring(0, 1).toUpperCase(Locale.ROOT);
        String color = String.format("%06x", Math.abs((name == null ? "Cricbook" : name).hashCode()) & 0xFFFFFF);
        return "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='128' height='128'><rect width='128' height='128' rx='32' fill='%23" + color + "'/><text x='50%' y='54%' text-anchor='middle' dominant-baseline='middle' font-family='Arial' font-size='48' font-weight='700' fill='white'>" + initial + "</text></svg>";
    }

    private MediaAssetResponse map(MediaAsset asset) {
        return MediaAssetResponse.builder()
                .id(asset.getId())
                .ownerType(asset.getOwnerType())
                .ownerId(asset.getOwnerId())
                .mediaType(asset.getMediaType())
                .imageUrl(asset.getImageUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .visibility(asset.getVisibility())
                .moderationStatus(asset.getModerationStatus())
                .moderationReason(asset.getModerationReason())
                .contentType(asset.getContentType())
                .fileSizeBytes(asset.getFileSizeBytes())
                .uploadedAt(asset.getUploadedAt() == null ? null : asset.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
