package com.example.Cricbook.controller;

import com.example.Cricbook.entity.Referral;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to access your rewards.");
        }
        return userDetails.getUsername();
    }

    // LOYALTY POINTS ENDPOINTS
    @GetMapping("/loyalty/balance")
    public ResponseEntity<Map<String, Integer>> getLoyaltyBalance(@AuthenticationPrincipal UserDetails userDetails) {
        int balance = loyaltyService.getPointsBalance(requireUsername(userDetails));
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    @GetMapping("/loyalty/history")
    public ResponseEntity<Map<String, String>> getLoyaltyHistory(@AuthenticationPrincipal UserDetails userDetails) {
        String history = loyaltyService.getPointsHistory(requireUsername(userDetails));
        return ResponseEntity.ok(Map.of("history", history));
    }

    @PostMapping("/loyalty/redeem")
    public ResponseEntity<Map<String, String>> redeemPoints(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Integer> request) {
        int points = request.getOrDefault("points", 0);
        loyaltyService.redeemPoints(requireUsername(userDetails), points);
        return ResponseEntity.ok(Map.of("message", "Points successfully converted to wallet cash."));
    }

    // REFERRALS ENDPOINTS
    @GetMapping("/referral/code")
    public ResponseEntity<Map<String, String>> getReferralCode(@AuthenticationPrincipal UserDetails userDetails) {
        String code = loyaltyService.generateReferralCode(requireUsername(userDetails));
        return ResponseEntity.ok(Map.of("code", code));
    }

    @PostMapping("/referral/claim")
    public ResponseEntity<Map<String, String>> claimReferral(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        String code = request.getOrDefault("code", "");
        loyaltyService.claimReferral(requireUsername(userDetails), code);
        return ResponseEntity.ok(Map.of("message", "Referral code successfully linked. Reward will be activated upon your first booking."));
    }

    @GetMapping("/referral/list")
    public ResponseEntity<List<Map<String, String>>> getMyReferrals(@AuthenticationPrincipal UserDetails userDetails) {
        List<Referral> referrals = loyaltyService.getMyReferrals(requireUsername(userDetails));
        List<Map<String, String>> responseList = referrals.stream()
                .map(r -> Map.of(
                        "inviteeEmail", r.getInvitee() != null ? r.getInvitee().getEmail() : "Joined Friend",
                        "inviteeName", r.getInvitee() != null ? r.getInvitee().getName() : "Friend",
                        "status", r.getStatus(),
                        "createdAt", r.getCreatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(responseList);
    }
}
