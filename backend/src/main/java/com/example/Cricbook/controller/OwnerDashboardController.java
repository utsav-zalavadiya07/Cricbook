package com.example.Cricbook.controller;

import com.example.Cricbook.dto.OwnerDashboardResponse;
import com.example.Cricbook.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerDashboardResponse> dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "days", defaultValue = "14") int days) {
        return ResponseEntity.ok(ownerDashboardService.getDashboard(userDetails.getUsername(), days));
    }
}
