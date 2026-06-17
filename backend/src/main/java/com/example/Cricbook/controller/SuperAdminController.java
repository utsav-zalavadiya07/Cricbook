package com.example.Cricbook.controller;

import com.example.Cricbook.dto.AdminAnalyticsResponse;
import com.example.Cricbook.dto.AdminReviewResponse;
import com.example.Cricbook.dto.AdminUserResponse;
import com.example.Cricbook.dto.TurfResponse;
import com.example.Cricbook.enums.TurfStatus;
import com.example.Cricbook.enums.UserStatus;
import com.example.Cricbook.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasAuthority('TURF_MODERATE')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/ops/analytics")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(superAdminService.getAnalytics());
    }

    @GetMapping("/owners")
    public ResponseEntity<List<AdminUserResponse>> getOwners() {
        return ResponseEntity.ok(superAdminService.getOwners());
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, UserStatus> request) {
        return ResponseEntity.ok(superAdminService.updateUserStatus(userId, request.get("status")));
    }

    @GetMapping("/turfs")
    public ResponseEntity<List<TurfResponse>> getTurfsForVerification() {
        return ResponseEntity.ok(superAdminService.getTurfsForVerification());
    }

    @PatchMapping("/turfs/{turfId}/status")
    public ResponseEntity<TurfResponse> updateTurfStatus(
            @PathVariable Long turfId,
            @RequestBody Map<String, TurfStatus> request) {
        return ResponseEntity.ok(superAdminService.updateTurfStatus(turfId, request.get("status")));
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<AdminReviewResponse>> getReviews() {
        return ResponseEntity.ok(superAdminService.getReviews());
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        superAdminService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
