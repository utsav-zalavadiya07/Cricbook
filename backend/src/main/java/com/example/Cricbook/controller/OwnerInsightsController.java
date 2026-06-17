package com.example.Cricbook.controller;

import com.example.Cricbook.dto.OwnerExpenseRequest;
import com.example.Cricbook.dto.OwnerExpenseResponse;
import com.example.Cricbook.dto.OwnerInsightsResponse;
import com.example.Cricbook.service.OwnerInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OwnerInsightsController {
    private final OwnerInsightsService ownerInsightsService;

    @GetMapping("/owner/insights")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerInsightsResponse> ownerInsights(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerInsightsService.getOwnerInsights(userDetails.getUsername()));
    }

    @GetMapping("/owner/insights/expenses")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<OwnerExpenseResponse>> expenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ownerInsightsService.getExpenses(userDetails.getUsername(), start, end));
    }

    @PostMapping("/owner/insights/expenses")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerExpenseResponse> addExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OwnerExpenseRequest request) {
        return ResponseEntity.ok(ownerInsightsService.addExpense(userDetails.getUsername(), request));
    }

    @PostMapping("/owner/insights/coach")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerInsightsResponse.BusinessCoachResponse> askCoach(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OwnerInsightsResponse.BusinessCoachRequest request) {
        return ResponseEntity.ok(ownerInsightsService.askCoach(userDetails.getUsername(), request));
    }

    @GetMapping("/admin/owner-insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OwnerInsightsResponse> platformOwnerInsights() {
        return ResponseEntity.ok(ownerInsightsService.getPlatformInsights());
    }
}
