package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trust")
@RequiredArgsConstructor
public class TrustController {
    private final TrustScoreService trustScoreService;

    @GetMapping("/me")
    public ResponseEntity<TrustScoreResponse> myTrustProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(trustScoreService.getMyProfile(requireUsername(userDetails)));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<TrustScoreResponse> trustProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(trustScoreService.getProfile(userId));
    }

    @GetMapping("/profile/{userId}/attendance")
    public ResponseEntity<List<TrustAttendanceResponse>> attendanceHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(trustScoreService.attendanceHistory(userId));
    }

    @PostMapping("/reports")
    public ResponseEntity<TrustReportResponse> submitReport(@AuthenticationPrincipal UserDetails userDetails,
                                                            @RequestBody TrustReportRequest request) {
        return ResponseEntity.ok(trustScoreService.submitReport(requireUsername(userDetails), request));
    }

    @PostMapping("/owner/bookings/{bookingId}/attendance")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TrustAttendanceResponse> updateAttendance(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @PathVariable Long bookingId,
                                                                    @RequestBody TrustAttendanceRequest request) {
        return ResponseEntity.ok(trustScoreService.updateAttendance(requireUsername(userDetails), bookingId, request));
    }

    @GetMapping("/owner/risk-board")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<TrustScoreResponse>> ownerRiskBoard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(trustScoreService.ownerRiskBoard(requireUsername(userDetails)));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrustAnalyticsResponse> analytics() {
        return ResponseEntity.ok(trustScoreService.analytics());
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TrustReportResponse>> openReports() {
        return ResponseEntity.ok(trustScoreService.openReports());
    }

    @PostMapping("/admin/reports/{reportId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrustReportResponse> moderateReport(@PathVariable Long reportId,
                                                              @RequestBody TrustModerationRequest request) {
        return ResponseEntity.ok(trustScoreService.moderateReport(reportId, request));
    }

    @PostMapping("/admin/profiles/{userId}/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TrustScoreResponse> recalculate(@PathVariable Long userId) {
        return ResponseEntity.ok(trustScoreService.calculateScore(userId));
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw com.example.Cricbook.exception.ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }
}
