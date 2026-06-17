package com.example.Cricbook.controller;

import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.CrmGrowthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CrmGrowthController {

    private final CrmGrowthService crmGrowthService;

    @GetMapping("/growth/dashboard")
    public ResponseEntity<Map<String, Object>> playerGrowthDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(crmGrowthService.playerDashboard(requireUsername(userDetails)));
    }

    @GetMapping("/admin/growth/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> adminGrowthDashboard() {
        return ResponseEntity.ok(crmGrowthService.adminDashboard());
    }

    @GetMapping("/admin/growth/segments")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> adminGrowthSegments() {
        return ResponseEntity.ok(crmGrowthService.segments());
    }

    @GetMapping("/admin/growth/automation")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<List<Map<String, Object>>> adminGrowthAutomation() {
        return ResponseEntity.ok(crmGrowthService.automationPlaybooks());
    }

    @GetMapping("/admin/growth/reconciliation")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> adminGrowthReconciliation() {
        return ResponseEntity.ok(crmGrowthService.reconciliation());
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to access your growth dashboard.");
        }
        return userDetails.getUsername();
    }
}
