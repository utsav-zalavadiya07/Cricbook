package com.example.Cricbook.controller.ai;

import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.ai.AIIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AIIntelligenceController {

    private final AIIntelligenceService intelligenceService;
    private final UserRepository userRepository;

    @GetMapping("/api/v1/ai/intelligence")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> userIntelligence(@AuthenticationPrincipal UserDetails userDetails) {
        User user = requireUser(userDetails);
        return ResponseEntity.ok(intelligenceService.userIntelligence(user.getId()));
    }

    @PostMapping("/api/v1/ai/recommendations/feedback")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> feedback(@AuthenticationPrincipal UserDetails userDetails,
                                                        @RequestBody Map<String, Object> request) {
        User user = requireUser(userDetails);
        return ResponseEntity.ok(intelligenceService.recordFeedback(user.getId(), request));
    }

    @GetMapping("/api/v1/ai/forecasts/revenue")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> userForecasts() {
        return ResponseEntity.ok(intelligenceService.forecasts());
    }

    @GetMapping("/api/v1/admin/ai/dashboard")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('AI_INSIGHTS_VIEW') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(intelligenceService.adminDashboard());
    }

    @GetMapping("/api/v1/admin/ai/governance")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('AI_INSIGHTS_VIEW') or hasAuthority('SECURITY_AUDIT_VIEW')")
    public ResponseEntity<Map<String, Object>> governance() {
        return ResponseEntity.ok(intelligenceService.governance());
    }

    @GetMapping("/api/v1/admin/ai/forecasts")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('FORECAST_VIEW') or hasAuthority('AI_INSIGHTS_VIEW')")
    public ResponseEntity<?> forecasts() {
        return ResponseEntity.ok(intelligenceService.forecasts());
    }

    @GetMapping("/api/v1/admin/ai/review-insights")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('AI_INSIGHTS_VIEW') or hasAuthority('REVIEW_MODERATE')")
    public ResponseEntity<Map<String, Object>> reviewInsights() {
        return ResponseEntity.ok(intelligenceService.reviewInsights());
    }

    @PostMapping("/api/v1/admin/ai/copilot/query")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('AI_INSIGHTS_VIEW') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<Map<String, Object>> copilot(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestBody Map<String, String> request) {
        String actor = userDetails == null ? "admin" : userDetails.getUsername();
        return ResponseEntity.ok(intelligenceService.copilotQuery(actor, request.get("query")));
    }

    private User requireUser(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw ApiException.unauthorized("Please sign in again to continue.");
        }
        return userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }
}
