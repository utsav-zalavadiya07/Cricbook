package com.example.Cricbook.controller;

import com.example.Cricbook.dto.PlayerPerformanceLeaderboardResponse;
import com.example.Cricbook.dto.PlayerPerformanceResponse;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.PlayerPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/player-performance")
@RequiredArgsConstructor
public class PlayerPerformanceController {

    private final PlayerPerformanceService playerPerformanceService;

    @GetMapping("/me")
    public ResponseEntity<PlayerPerformanceResponse> myPerformance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(playerPerformanceService.myPerformance(requireUsername(userDetails)));
    }

    @GetMapping("/players/{userId}")
    public ResponseEntity<PlayerPerformanceResponse> playerPerformance(@PathVariable Long userId) {
        return ResponseEntity.ok(playerPerformanceService.playerPerformance(userId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<PlayerPerformanceLeaderboardResponse>> leaderboard(@RequestParam(required = false) String type,
                                                                                  @RequestParam(required = false) String city,
                                                                                  @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(playerPerformanceService.leaderboard(type, city, limit));
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Please sign in to view player performance.");
        }
        return userDetails.getUsername();
    }
}
