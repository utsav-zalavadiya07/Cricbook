package com.example.Cricbook.controller;

import com.example.Cricbook.dto.BlockSlotRequest;
import com.example.Cricbook.dto.CreateSlotRequest;
import com.example.Cricbook.dto.SlotPriceOverrideRequest;
import com.example.Cricbook.dto.SlotAvailabilityResponse;
import com.example.Cricbook.dto.SlotCalendarDateResponse;
import com.example.Cricbook.dto.SlotResponse;
import com.example.Cricbook.dto.UpdateSlotRequest;
import com.example.Cricbook.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/turfs/{turfId}")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    @PostMapping("/slots")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<SlotResponse>> createSlots(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateSlotRequest request) {
        String email = userDetails.getUsername();
        List<SlotResponse> response = slotService.createSlots(turfId, email, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/slots")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(slotService.getSlots(turfId, userDetails.getUsername()));
    }

    @PutMapping("/slots/{slotId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<SlotResponse> updateSlot(
            @PathVariable Long turfId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateSlotRequest request) {
        return ResponseEntity.ok(slotService.updateSlot(turfId, slotId, userDetails.getUsername(), request));
    }

    @PostMapping("/slots/{slotId}/disable")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<SlotResponse> disableSlot(
            @PathVariable Long turfId,
            @PathVariable Long slotId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(slotService.disableSlot(turfId, slotId, userDetails.getUsername()));
    }

    @PostMapping("/slots/pricing")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> applyPriceOverride(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SlotPriceOverrideRequest request) {
        slotService.applyPriceOverride(turfId, userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/block")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> blockSlots(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BlockSlotRequest request) {
        String email = userDetails.getUsername();
        slotService.blockSlots(turfId, email, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unblock")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> unblockSlots(
            @PathVariable Long turfId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BlockSlotRequest request) {
        String email = userDetails.getUsername();
        slotService.unblockSlots(turfId, email, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/availability")
    public ResponseEntity<List<SlotAvailabilityResponse>> getAvailability(
            @PathVariable Long turfId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SlotAvailabilityResponse> response = slotService.getAvailability(turfId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/availability/calendar")
    public ResponseEntity<List<SlotCalendarDateResponse>> getCalendar(
            @PathVariable Long turfId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(slotService.getCalendar(turfId, startDate, endDate));
    }
}
