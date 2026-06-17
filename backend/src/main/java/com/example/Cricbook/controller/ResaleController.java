package com.example.Cricbook.controller;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.service.ResaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resale")
@RequiredArgsConstructor
public class ResaleController {
    private final ResaleService resaleService;

    @PostMapping("/create")
    public ResponseEntity<ResaleListingResponse> createListing(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateResaleListingRequest request) {
        return ResponseEntity.ok(resaleService.createListing(userDetails.getUsername(), request));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ResaleListingResponse>> activeListings(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(resaleService.activeListings(city));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<ResaleListingResponse>> recommendations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resaleService.recommendations(userDetails.getUsername()));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<ResaleListingResponse>> recommendationsCompat(@AuthenticationPrincipal UserDetails userDetails) {
        return recommendations(userDetails);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ResaleListingResponse>> myListings(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resaleService.myListings(userDetails.getUsername()));
    }

    @GetMapping("/my-listings")
    public ResponseEntity<List<ResaleListingResponse>> myListingsCompat(@AuthenticationPrincipal UserDetails userDetails) {
        return myListings(userDetails);
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<ResaleListingResponse> getListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(resaleService.getListing(listingId));
    }

    @PostMapping("/{listingId}/buy")
    public ResponseEntity<ResaleListingResponse> buyListing(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long listingId,
            @RequestBody(required = false) BuyResaleListingRequest request) {
        return ResponseEntity.ok(resaleService.buyListing(userDetails.getUsername(), listingId, request));
    }

    @PostMapping("/buy")
    public ResponseEntity<ResaleListingResponse> buyListingCompat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long listingId,
            @RequestBody(required = false) BuyResaleListingRequest request) {
        return ResponseEntity.ok(resaleService.buyListing(userDetails.getUsername(), listingId, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<BookingTransferResponse> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TransferBookingRequest request) {
        return ResponseEntity.ok(resaleService.createDirectTransfer(userDetails.getUsername(), request));
    }

    @PostMapping("/waitlist")
    public ResponseEntity<SlotWaitlistResponse> joinWaitlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody JoinWaitlistRequest request) {
        return ResponseEntity.ok(resaleService.joinWaitlist(userDetails.getUsername(), request));
    }

    @GetMapping("/waitlist/my")
    public ResponseEntity<List<SlotWaitlistResponse>> myWaitlist(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resaleService.myWaitlist(userDetails.getUsername()));
    }

    @GetMapping("/waitlist")
    public ResponseEntity<List<SlotWaitlistResponse>> myWaitlistCompat(@AuthenticationPrincipal UserDetails userDetails) {
        return myWaitlist(userDetails);
    }

    @GetMapping("/owner/rules")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ResaleRuleResponse> ownerRules(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resaleService.getOwnerRules(userDetails.getUsername()));
    }

    @PutMapping("/owner/rules")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ResaleRuleResponse> updateOwnerRules(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ResaleRuleRequest request) {
        return ResponseEntity.ok(resaleService.updateOwnerRules(userDetails.getUsername(), request));
    }

    @GetMapping("/owner/analytics")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<ResaleAnalyticsResponse> ownerAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(resaleService.ownerAnalytics(userDetails.getUsername()));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResaleAnalyticsResponse> platformAnalytics() {
        return ResponseEntity.ok(resaleService.platformAnalytics());
    }
}
