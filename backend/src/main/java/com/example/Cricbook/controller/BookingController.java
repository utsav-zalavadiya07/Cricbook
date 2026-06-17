package com.example.Cricbook.controller;

import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.ConfirmBookingRequest;
import com.example.Cricbook.dto.CreateBatchBookingRequest;
import com.example.Cricbook.dto.CreateBookingRequest;
import com.example.Cricbook.dto.OwnerBookingActionRequest;
import com.example.Cricbook.dto.OwnerBookingDetailsResponse;
import com.example.Cricbook.dto.OwnerBookingOperationsSummary;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<BookingResponse>> createBatchBooking(@RequestBody CreateBatchBookingRequest request) {
        return ResponseEntity.ok(bookingService.createBatchBooking(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        return ResponseEntity.ok(bookingService.getMyBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponse>> getOwnerBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long turfId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(bookingService.getOwnerBookings(status, date, turfId, query));
    }

    @GetMapping("/owner/summary")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerBookingOperationsSummary> getOwnerBookingSummary() {
        return ResponseEntity.ok(bookingService.getOwnerOperationsSummary());
    }

    @GetMapping("/owner/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> getOwnerBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getOwnerBooking(id));
    }

    @GetMapping("/owner/{id}/details")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<OwnerBookingDetailsResponse> getOwnerBookingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getOwnerBookingDetails(id));
    }

    @PostMapping("/owner/{id}/cancel")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> cancelOwnerBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelOwnerBooking(id));
    }

    @PostMapping("/owner/{id}/check-in")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> checkInOwnerBooking(@PathVariable Long id, @RequestBody(required = false) OwnerBookingActionRequest request) {
        return ResponseEntity.ok(bookingService.checkInOwnerBooking(id, request));
    }

    @PostMapping("/owner/{id}/complete")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> completeOwnerBooking(@PathVariable Long id, @RequestBody(required = false) OwnerBookingActionRequest request) {
        return ResponseEntity.ok(bookingService.completeOwnerBooking(id, request));
    }

    @PostMapping("/owner/{id}/no-show")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<BookingResponse> markOwnerNoShow(@PathVariable Long id, @RequestBody(required = false) OwnerBookingActionRequest request) {
        return ResponseEntity.ok(bookingService.markOwnerNoShow(id, request));
    }

    @PostMapping("/owner/{id}/notes")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> addOwnerBookingNote(@PathVariable Long id, @RequestBody OwnerBookingActionRequest request) {
        return ResponseEntity.ok(bookingService.addOwnerBookingNote(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            @RequestBody ConfirmBookingRequest request) {
        return ResponseEntity.ok(bookingService.confirmBooking(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
