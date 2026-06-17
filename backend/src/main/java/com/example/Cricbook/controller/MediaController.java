package com.example.Cricbook.controller;

import com.example.Cricbook.dto.MediaAssetResponse;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @GetMapping("/profile/me")
    public ResponseEntity<MediaAssetResponse> getMyProfileMedia(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mediaService.getMyProfileMedia(requireUsername(userDetails)));
    }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetResponse> uploadProfilePhoto(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @RequestPart("file") MultipartFile file,
                                                                 @RequestParam(defaultValue = "PUBLIC") String visibility) {
        return ResponseEntity.ok(mediaService.uploadProfilePhoto(requireUsername(userDetails), file, visibility));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<MediaAssetResponse> removeProfilePhoto(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mediaService.removeProfilePhoto(requireUsername(userDetails)));
    }

    @PostMapping(value = "/owner/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<MediaAssetResponse> uploadOwnerLogo(@AuthenticationPrincipal UserDetails userDetails,
                                                              @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.uploadOwnerLogo(requireUsername(userDetails), file));
    }

    @DeleteMapping("/owner/logo")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<MediaAssetResponse> removeOwnerLogo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mediaService.removeOwnerLogo(requireUsername(userDetails)));
    }

    @PostMapping(value = "/teams/{teamId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetResponse> uploadTeamLogo(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long teamId,
                                                             @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.uploadTeamLogo(requireUsername(userDetails), teamId, file));
    }

    @DeleteMapping("/teams/{teamId}/logo")
    public ResponseEntity<MediaAssetResponse> removeTeamLogo(@AuthenticationPrincipal UserDetails userDetails,
                                                             @PathVariable Long teamId) {
        return ResponseEntity.ok(mediaService.removeTeamLogo(requireUsername(userDetails), teamId));
    }

    @GetMapping("/admin/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MediaAssetResponse>> moderationQueue() {
        return ResponseEntity.ok(mediaService.moderationQueue());
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }
}
