package com.example.Cricbook.controller;

import com.example.Cricbook.dto.OwnerFinanceDashboardResponse;
import com.example.Cricbook.service.OwnerFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/owner/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
public class OwnerFinanceController {
    private final OwnerFinanceService ownerFinanceService;

    @GetMapping("/dashboard")
    public ResponseEntity<OwnerFinanceDashboardResponse> dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long turfId
    ) {
        return ResponseEntity.ok(ownerFinanceService.getDashboard(userDetails.getUsername(), start, end, turfId));
    }

    @PostMapping("/settlements/generate")
    public ResponseEntity<Map<String, Object>> generateSettlements(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ownerFinanceService.generateSettlements(userDetails.getUsername()));
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long turfId
    ) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=owner-finance-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(ownerFinanceService.exportCsv(userDetails.getUsername(), start, end, turfId));
    }
}
