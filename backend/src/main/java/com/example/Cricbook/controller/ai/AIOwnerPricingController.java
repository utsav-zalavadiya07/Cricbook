package com.example.Cricbook.controller.ai;

import com.example.Cricbook.entity.PricingRule;
import com.example.Cricbook.entity.PricingHistory;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.service.ai.AIDynamicPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owner/pricing")
@RequiredArgsConstructor
public class AIOwnerPricingController {

    private final AIDynamicPricingService pricingService;

    @GetMapping("/rules")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<PricingRule>> getRules(@AuthenticationPrincipal UserDetails userDetails,
                                                       @RequestParam("turfId") Long turfId) {
        List<PricingRule> rules = pricingService.getRules(requireUsername(userDetails), turfId);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/toggle")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> toggleRule(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody Map<String, Object> request) {
        Long turfId = parseLong(request.get("turfId"), "Turf ID is required.");
        String ruleName = parseString(request.get("ruleName"), "Pricing rule name is required.");
        boolean active = parseBoolean(request.get("active"));
        BigDecimal adjustmentPercent = parseOptionalBigDecimal(request.get("adjustmentPercent"));

        pricingService.toggleRule(requireUsername(userDetails), turfId, ruleName, active, adjustmentPercent);
        return ResponseEntity.ok(Map.of("message", "Rule toggled successfully", "ruleName", ruleName, "active", active));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<PricingHistory>> getHistory(@AuthenticationPrincipal UserDetails userDetails,
                                                           @RequestParam("turfId") Long turfId) {
        List<PricingHistory> histories = pricingService.getPricingHistory(requireUsername(userDetails), turfId);
        return ResponseEntity.ok(histories);
    }

    private String requireUsername(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null || userDetails.getUsername().isBlank()) {
            throw ApiException.unauthorized("Please sign in again to continue.");
        }
        return userDetails.getUsername();
    }

    private Long parseLong(Object value, String message) {
        if (value == null) {
            throw ApiException.badRequest(message);
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest(message);
        }
    }

    private String parseString(Object value, String message) {
        if (value == null || value.toString().isBlank()) {
            throw ApiException.badRequest(message);
        }
        return value.toString();
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            throw ApiException.badRequest("Rule active status is required.");
        }
        return Boolean.parseBoolean(value.toString());
    }

    private BigDecimal parseOptionalBigDecimal(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest("Adjustment percent must be a valid number.");
        }
    }
}
