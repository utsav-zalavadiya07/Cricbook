package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SplitPaymentDtos;
import com.example.Cricbook.service.SplitPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SplitPaymentController {
    private final SplitPaymentService splitPaymentService;

    @PostMapping("/payment/split")
    public ResponseEntity<SplitPaymentDtos.SplitPaymentResponse> createSplit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SplitPaymentDtos.CreateSplitRequest request) {
        return ResponseEntity.ok(splitPaymentService.createSplit(userDetails.getUsername(), request));
    }

    @GetMapping("/payment/split/{bookingId}")
    public ResponseEntity<SplitPaymentDtos.SplitPaymentResponse> getSplit(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(splitPaymentService.getBookingSplit(userDetails.getUsername(), bookingId));
    }

    @GetMapping("/payment/splits/my")
    public ResponseEntity<List<SplitPaymentDtos.PaymentSplitResponse>> mySplits(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(splitPaymentService.mySplits(userDetails.getUsername()));
    }

    @PostMapping("/payment/pay-share")
    public ResponseEntity<SplitPaymentDtos.SplitPaymentResponse> payShare(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SplitPaymentDtos.PayShareRequest request) {
        return ResponseEntity.ok(splitPaymentService.payShare(userDetails.getUsername(), request));
    }

    @PostMapping("/payment/split/{bookingId}/remind")
    public ResponseEntity<SplitPaymentDtos.SplitPaymentResponse> remind(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(splitPaymentService.sendReminders(userDetails.getUsername(), bookingId));
    }

    @GetMapping("/owner/payment-rules")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<SplitPaymentDtos.PaymentRuleResponse> ownerRules(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(splitPaymentService.getOwnerRules(userDetails.getUsername()));
    }

    @PutMapping("/owner/payment-rules")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<SplitPaymentDtos.PaymentRuleResponse> updateOwnerRules(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SplitPaymentDtos.PaymentRuleRequest request) {
        return ResponseEntity.ok(splitPaymentService.updateOwnerRules(userDetails.getUsername(), request));
    }

    @PostMapping("/payment/recurring-collections")
    public ResponseEntity<SplitPaymentDtos.RecurringCollectionResponse> createRecurring(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SplitPaymentDtos.RecurringCollectionRequest request) {
        return ResponseEntity.ok(splitPaymentService.createRecurring(userDetails.getUsername(), request));
    }

    @GetMapping("/payment/analytics")
    public ResponseEntity<SplitPaymentDtos.SplitAnalyticsResponse> analytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(splitPaymentService.analytics(userDetails.getUsername()));
    }

    @GetMapping("/admin/payment-monitoring")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SplitPaymentDtos.SplitAnalyticsResponse> adminAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(splitPaymentService.analytics(userDetails.getUsername()));
    }
}
