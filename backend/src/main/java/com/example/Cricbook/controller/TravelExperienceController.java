package com.example.Cricbook.controller;

import com.example.Cricbook.dto.TravelExperienceRequests;
import com.example.Cricbook.enums.TravelEventType;
import com.example.Cricbook.service.TravelExperienceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TravelExperienceController {

    private static final String ADMIN_TRAVEL_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_TRAVEL_READ') or hasAuthority('ADMIN_TRAVEL_FULL') or hasAuthority('REPORT_VIEW')";

    private final TravelExperienceService travelExperienceService;

    @GetMapping("/travel/packages")
    public ResponseEntity<List<Map<String, Object>>> packages(@RequestParam(required = false) String query,
                                                              @RequestParam(required = false) String sport,
                                                              @RequestParam(required = false) String originCity,
                                                              @RequestParam(required = false) String destination,
                                                              @RequestParam(required = false) BigDecimal maxPrice) {
        return ResponseEntity.ok(travelExperienceService.packageMarketplace(query, sport, originCity, destination, maxPrice));
    }

    @GetMapping("/travel/events")
    public ResponseEntity<List<Map<String, Object>>> events(@RequestParam(required = false) TravelEventType eventType,
                                                            @RequestParam(required = false) String query,
                                                            @RequestParam(required = false) String sport,
                                                            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(travelExperienceService.eventMarketplace(eventType, query, sport, city));
    }

    @GetMapping("/travel/experiences")
    public ResponseEntity<List<Map<String, Object>>> experiences(@RequestParam(required = false) String query,
                                                                 @RequestParam(required = false) String sport,
                                                                 @RequestParam(required = false) String city) {
        return ResponseEntity.ok(travelExperienceService.eventMarketplace(TravelEventType.EXPERIENCE, query, sport, city));
    }

    @GetMapping("/travel/accommodations")
    public ResponseEntity<List<Map<String, Object>>> accommodations(@RequestParam(required = false) String city,
                                                                    @RequestParam(required = false) String accommodationType) {
        return ResponseEntity.ok(travelExperienceService.accommodations(city, accommodationType));
    }

    @GetMapping("/travel/my")
    public ResponseEntity<Map<String, Object>> myTravel(Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.myTravel(actor(authentication)));
    }

    @PostMapping("/travel/packages/{id}/book")
    public ResponseEntity<Map<String, Object>> bookPackage(@PathVariable Long id,
                                                           @RequestBody(required = false) TravelExperienceRequests.PackageBookingRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.bookPackage(id, request, actor(authentication)));
    }

    @PostMapping("/travel/bookings/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPackageBooking(@PathVariable Long id,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.cancelPackageBooking(id, actor(authentication)));
    }

    @PostMapping("/travel/events/{id}/tickets")
    public ResponseEntity<Map<String, Object>> buyTicket(@PathVariable Long id,
                                                         @RequestBody TravelExperienceRequests.TicketPurchaseRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.buyTicket(id, request, actor(authentication)));
    }

    @PostMapping("/travel/accommodations/{id}/book")
    public ResponseEntity<Map<String, Object>> bookAccommodation(@PathVariable Long id,
                                                                 @RequestBody TravelExperienceRequests.AccommodationBookingRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.bookAccommodation(id, request, actor(authentication)));
    }

    @PostMapping("/travel/partners")
    public ResponseEntity<Map<String, Object>> registerPartner(@RequestBody TravelExperienceRequests.PartnerRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.registerPartner(request, actor(authentication)));
    }

    @GetMapping("/admin/travel/dashboard")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(travelExperienceService.dashboard());
    }

    @PostMapping("/admin/travel/partners")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> createPartner(@RequestBody TravelExperienceRequests.PartnerRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.registerPartner(request, actor(authentication)));
    }

    @PostMapping("/admin/travel/partners/{id}/status")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> updatePartnerStatus(@PathVariable Long id,
                                                                   @RequestBody(required = false) TravelExperienceRequests.PartnerStatusRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.updatePartnerStatus(id, request, actor(authentication)));
    }

    @PostMapping("/admin/travel/packages")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> createPackage(@RequestBody TravelExperienceRequests.PackageRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.createPackage(request, actor(authentication)));
    }

    @PostMapping("/admin/travel/events")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> createEvent(@RequestBody TravelExperienceRequests.EventRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.createEvent(request, actor(authentication)));
    }

    @PostMapping("/admin/travel/events/{id}/ticket-types")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> createTicketType(@PathVariable Long id,
                                                                @RequestBody TravelExperienceRequests.TicketTypeRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.createTicketType(id, request, actor(authentication)));
    }

    @PostMapping("/admin/travel/tickets/validate")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> validateTicket(@RequestParam String qrCode,
                                                              Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.validateTicketQr(qrCode, actor(authentication)));
    }

    @PostMapping("/admin/travel/accommodations")
    @PreAuthorize(ADMIN_TRAVEL_ACCESS)
    public ResponseEntity<Map<String, Object>> createAccommodation(@RequestBody TravelExperienceRequests.AccommodationRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(travelExperienceService.createAccommodation(request, actor(authentication)));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
