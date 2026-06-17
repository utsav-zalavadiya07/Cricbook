package com.example.Cricbook.controller;

import com.example.Cricbook.dto.PlayerSettingsRequest;
import com.example.Cricbook.dto.PlayerSettingsResponse;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.PlayerSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class PlayerSettingsController {

    private final PlayerSettingsService playerSettingsService;

    @GetMapping
    public ResponseEntity<PlayerSettingsResponse> getSettings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(playerSettingsService.getSettings(requireUsername(userDetails)));
    }

    @PutMapping
    public ResponseEntity<PlayerSettingsResponse> updateSettings(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @RequestBody PlayerSettingsRequest request) {
        return ResponseEntity.ok(playerSettingsService.updateSettings(requireUsername(userDetails), request));
    }

    @PostMapping("/data-export")
    public ResponseEntity<PlayerSettingsResponse> requestDataExport(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(playerSettingsService.requestDataExport(requireUsername(userDetails)));
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }
}
