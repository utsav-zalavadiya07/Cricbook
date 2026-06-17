package com.example.Cricbook.controller;

import com.example.Cricbook.entity.HolidayMaster;
import com.example.Cricbook.entity.HolidayPricingRule;
import com.example.Cricbook.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/turfs/{turfId}")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping("/holiday-rule")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<HolidayPricingRule> getRule(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(holidayService.getRule(turfId, userDetails.getUsername()));
    }

    @PostMapping("/holiday-rule")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<HolidayPricingRule> saveRule(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody HolidayPricingRule rule) {
        return ResponseEntity.ok(holidayService.saveRule(turfId, userDetails.getUsername(), rule));
    }

    @GetMapping("/upcoming-holidays")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<HolidayMaster>> getUpcomingHolidays(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(holidayService.getUpcomingHolidays(turfId, userDetails.getUsername()));
    }
}
