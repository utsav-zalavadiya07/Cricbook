package com.example.Cricbook.controller;

import com.example.Cricbook.dto.WeatherDtos.*;
import com.example.Cricbook.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {
    private final WeatherService weatherService;

    @GetMapping("/forecast")
    public ResponseEntity<ForecastResponse> forecast(@RequestParam(required = false) String city,
                                                     @RequestParam(required = false) LocalDate date,
                                                     @RequestParam(required = false) Integer hour) {
        return ResponseEntity.ok(weatherService.forecast(city, date, hour));
    }

    @GetMapping("/forecast/day")
    public ResponseEntity<List<ForecastResponse>> dayForecast(@RequestParam(required = false) String city,
                                                              @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(weatherService.dayForecast(city, date));
    }

    @GetMapping("/playability")
    public ResponseEntity<PlayabilityResponse> playability(@RequestParam Long turfId,
                                                           @RequestParam Long slotId,
                                                           @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(weatherService.playability(turfId, slotId, date));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<WeatherDashboardResponse> playerDashboard(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @RequestParam(required = false) String city) {
        return ResponseEntity.ok(weatherService.playerDashboard(userDetails.getUsername(), city));
    }

    @PostMapping("/reschedule/{bookingId}")
    public ResponseEntity<WeatherRescheduleResponse> suggestReschedule(@AuthenticationPrincipal UserDetails userDetails,
                                                                       @PathVariable Long bookingId) {
        return ResponseEntity.ok(weatherService.suggestReschedule(userDetails.getUsername(), bookingId));
    }

    @PostMapping("/refund/{bookingId}/evaluate")
    public ResponseEntity<WeatherRefundResponse> evaluateRefund(@AuthenticationPrincipal UserDetails userDetails,
                                                                @PathVariable Long bookingId) {
        return ResponseEntity.ok(weatherService.evaluateRefund(userDetails.getUsername(), bookingId));
    }

    @GetMapping("/owner/policy")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<WeatherPolicyResponse> ownerPolicy(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(weatherService.getOwnerPolicy(userDetails.getUsername()));
    }

    @PutMapping("/owner/policy")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<WeatherPolicyResponse> updateOwnerPolicy(@AuthenticationPrincipal UserDetails userDetails,
                                                                   @RequestBody WeatherPolicyRequest request) {
        return ResponseEntity.ok(weatherService.updateOwnerPolicy(userDetails.getUsername(), request));
    }

    @PutMapping("/owner/environment")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<TurfEnvironmentResponse> updateEnvironment(@AuthenticationPrincipal UserDetails userDetails,
                                                                     @RequestBody TurfEnvironmentRequest request) {
        return ResponseEntity.ok(weatherService.updateEnvironment(userDetails.getUsername(), request));
    }

    @GetMapping("/owner/dashboard")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<WeatherDashboardResponse> ownerDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(weatherService.ownerDashboard(userDetails.getUsername()));
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WeatherDashboardResponse> adminDashboard(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(weatherService.adminDashboard(city));
    }
}
