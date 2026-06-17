package com.example.Cricbook.controller;

import com.example.Cricbook.dto.GamificationDtos.*;
import com.example.Cricbook.enums.LeaderboardType;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {
    private final GamificationService gamificationService;

    @GetMapping("/profile")
    public ResponseEntity<GamificationProfileResponse> profile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(gamificationService.profile(requireUsername(userDetails)));
    }

    @PostMapping("/xp")
    public ResponseEntity<GamificationProfileResponse> awardXp(@AuthenticationPrincipal UserDetails userDetails,
                                                               @RequestBody AwardXpRequest request) {
        return ResponseEntity.ok(gamificationService.awardXp(requireUsername(userDetails), request));
    }

    @PostMapping("/rewards/{rewardId}/claim")
    public ResponseEntity<RewardResponse> claimReward(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long rewardId) {
        return ResponseEntity.ok(gamificationService.claimReward(requireUsername(userDetails), rewardId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryResponse>> leaderboard(@RequestParam(required = false) String city,
                                                                      @RequestParam(required = false) LeaderboardType type) {
        return ResponseEntity.ok(gamificationService.leaderboard(city, type));
    }

    @GetMapping("/owner/analytics")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<GamificationAnalyticsResponse> ownerAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(gamificationService.ownerAnalytics(requireUsername(userDetails)));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GamificationAnalyticsResponse> adminAnalytics() {
        return ResponseEntity.ok(gamificationService.adminAnalytics());
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to access gamification.");
        }
        return userDetails.getUsername();
    }
}
