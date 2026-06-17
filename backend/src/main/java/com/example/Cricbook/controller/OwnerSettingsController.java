package com.example.Cricbook.controller;

import com.example.Cricbook.dto.OwnerSettingsRequest;
import com.example.Cricbook.dto.OwnerSettingsResponse;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.OwnerSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner/settings")
@RequiredArgsConstructor
public class OwnerSettingsController {

    private final OwnerSettingsService ownerSettingsService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerSettingsResponse> getSettings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerSettingsService.getSettings(requireUsername(userDetails)));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerSettingsResponse> updateSettings(@AuthenticationPrincipal UserDetails userDetails,
                                                                @RequestBody OwnerSettingsRequest request) {
        return ResponseEntity.ok(ownerSettingsService.updateSettings(requireUsername(userDetails), request));
    }

    @PostMapping("/data-export")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerSettingsResponse> requestExport(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerSettingsService.requestExport(requireUsername(userDetails)));
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }
}
