package com.example.Cricbook.controller;

import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.RebookDtos.*;
import com.example.Cricbook.service.RebookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class RebookController {
    private final RebookService rebookService;

    @PostMapping("/rebook")
    public ResponseEntity<BookingResponse> rebook(@AuthenticationPrincipal UserDetails userDetails,
                                                  @RequestBody RebookRequest request) {
        return ResponseEntity.ok(rebookService.rebook(userDetails.getUsername(), request));
    }

    @GetMapping("/frequent")
    public ResponseEntity<List<FrequentBookingResponse>> frequent(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.frequentBookings(userDetails.getUsername()));
    }

    @GetMapping("/predictions")
    public ResponseEntity<List<BookingPredictionResponse>> predictions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.predictions(userDetails.getUsername()));
    }

    @PostMapping("/recurring")
    public ResponseEntity<RecurringBookingResponse> createRecurring(@AuthenticationPrincipal UserDetails userDetails,
                                                                    @RequestBody RecurringBookingRequest request) {
        return ResponseEntity.ok(rebookService.createRecurring(userDetails.getUsername(), request));
    }

    @GetMapping("/recurring")
    public ResponseEntity<List<RecurringBookingResponse>> recurring(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.myRecurring(userDetails.getUsername()));
    }

    @PostMapping("/templates")
    public ResponseEntity<BookingTemplateResponse> createTemplate(@AuthenticationPrincipal UserDetails userDetails,
                                                                  @RequestBody BookingTemplateRequest request) {
        return ResponseEntity.ok(rebookService.createTemplate(userDetails.getUsername(), request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<BookingTemplateResponse>> templates(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.myTemplates(userDetails.getUsername()));
    }

    @PostMapping("/auto-waitlist")
    public ResponseEntity<AutoWaitlistResponse> createAutoWaitlist(@AuthenticationPrincipal UserDetails userDetails,
                                                                   @RequestBody AutoWaitlistRequest request) {
        return ResponseEntity.ok(rebookService.createAutoWaitlist(userDetails.getUsername(), request));
    }

    @GetMapping("/auto-waitlist")
    public ResponseEntity<List<AutoWaitlistResponse>> autoWaitlist(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.myAutoWaitlists(userDetails.getUsername()));
    }

    @GetMapping("/intelligence")
    public ResponseEntity<BookingIntelligenceResponse> intelligence(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.myAnalytics(userDetails.getUsername()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<BookingIntelligenceResponse> analyticsCompat(@AuthenticationPrincipal UserDetails userDetails) {
        return intelligence(userDetails);
    }

    @GetMapping("/owner/intelligence")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BookingIntelligenceResponse> ownerIntelligence(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rebookService.ownerAnalytics(userDetails.getUsername()));
    }

    @GetMapping("/admin/intelligence")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingIntelligenceResponse> adminIntelligence() {
        return ResponseEntity.ok(rebookService.adminAnalytics());
    }
}
