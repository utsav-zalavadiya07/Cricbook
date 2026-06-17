package com.example.Cricbook.controller;

import com.example.Cricbook.dto.MatchmakingAnalyticsResponse;
import com.example.Cricbook.dto.MatchmakingCandidateResponse;
import com.example.Cricbook.dto.MatchmakingCreateRequest;
import com.example.Cricbook.dto.MatchmakingMatchResponse;
import com.example.Cricbook.service.MatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {
    private final MatchmakingService matchmakingService;

    @PostMapping("/create")
    public ResponseEntity<MatchmakingMatchResponse> create(@AuthenticationPrincipal UserDetails userDetails,
                                                           @RequestBody MatchmakingCreateRequest request) {
        return ResponseEntity.ok(matchmakingService.create(requireUsername(userDetails), request));
    }

    @GetMapping("/open")
    public ResponseEntity<List<MatchmakingMatchResponse>> open(@AuthenticationPrincipal UserDetails userDetails,
                                                               @RequestParam(required = false) String city,
                                                               @RequestParam(required = false) String skillLevel,
                                                               @RequestParam(defaultValue = "false") Boolean trustedOnly) {
        return ResponseEntity.ok(matchmakingService.open(requireUsername(userDetails), city, skillLevel, trustedOnly));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<MatchmakingMatchResponse>> nearby(@AuthenticationPrincipal UserDetails userDetails,
                                                                 @RequestParam(required = false) String city) {
        return ResponseEntity.ok(matchmakingService.nearby(requireUsername(userDetails), city));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<MatchmakingMatchResponse>> recommendations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchmakingService.recommendations(requireUsername(userDetails)));
    }

    @GetMapping("/matches/{matchId}/candidates")
    public ResponseEntity<List<MatchmakingCandidateResponse>> candidates(@AuthenticationPrincipal UserDetails userDetails,
                                                                         @PathVariable Long matchId,
                                                                         @RequestParam(required = false) String city,
                                                                         @RequestParam(required = false) String skillLevel,
                                                                         @RequestParam(defaultValue = "false") boolean trustedOnly) {
        return ResponseEntity.ok(matchmakingService.candidates(requireUsername(userDetails), matchId, city, skillLevel, trustedOnly));
    }

    @PostMapping("/matches/{matchId}/join")
    public ResponseEntity<MatchmakingMatchResponse> join(@AuthenticationPrincipal UserDetails userDetails,
                                                         @PathVariable Long matchId) {
        return ResponseEntity.ok(matchmakingService.join(requireUsername(userDetails), matchId));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<MatchmakingAnalyticsResponse> analytics() {
        return ResponseEntity.ok(matchmakingService.analytics());
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw com.example.Cricbook.exception.ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }
}
