package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SportsOsRequests;
import com.example.Cricbook.service.SportsOsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SportsOsController {

    private static final String ADMIN_SPORTS_OS_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('SPORTS_OS_ADMIN') or hasAuthority('GOVERNMENT_PORTAL_ADMIN') or hasAuthority('ADMIN_GOVERNANCE_FULL') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final SportsOsService sportsOsService;

    @GetMapping("/sports-os/dashboard")
    public ResponseEntity<Map<String, Object>> publicDashboard() {
        return ResponseEntity.ok(sportsOsService.dashboard());
    }

    @GetMapping("/sports-os/facilities")
    public ResponseEntity<List<Map<String, Object>>> facilities(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(sportsOsService.searchFacilities(query));
    }

    @GetMapping("/sports-os/facilities/{facilityId}")
    public ResponseEntity<Map<String, Object>> facilityDetails(@PathVariable Long facilityId) {
        return ResponseEntity.ok(sportsOsService.facilityDetails(facilityId));
    }

    @PostMapping("/sports-os/facilities")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> registerFacility(@RequestBody SportsOsRequests.FacilityRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.registerFacility(request, actor(authentication)));
    }

    @PostMapping("/sports-os/census")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> submitCensus(@RequestBody SportsOsRequests.CensusSubmissionRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.submitCensus(request, actor(authentication)));
    }

    @PostMapping("/sports-os/grants")
    public ResponseEntity<Map<String, Object>> applyGrant(@RequestBody SportsOsRequests.GrantApplicationRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.applyGrant(request, actor(authentication)));
    }

    @PostMapping("/sports-os/athletes")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> trackAthlete(@RequestBody SportsOsRequests.AthleteDevelopmentRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.trackAthlete(request, actor(authentication)));
    }

    @PostMapping("/sports-os/volunteers")
    public ResponseEntity<Map<String, Object>> registerVolunteer(@RequestBody SportsOsRequests.VolunteerRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.registerVolunteer(request, actor(authentication)));
    }

    @GetMapping("/admin/sports-os/dashboard")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(sportsOsService.adminDashboard());
    }

    @GetMapping("/admin/sports-os/facilities")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminFacilities() {
        return ResponseEntity.ok(sportsOsService.facilities());
    }

    @PostMapping("/admin/sports-os/facilities/{facilityId}/status")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> updateFacilityStatus(@PathVariable Long facilityId,
                                                                    @RequestBody SportsOsRequests.FacilityStatusRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.updateFacilityStatus(facilityId, request, actor(authentication)));
    }

    @GetMapping("/admin/sports-os/census")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminCensus() {
        return ResponseEntity.ok(sportsOsService.censusSubmissions());
    }

    @GetMapping("/admin/sports-os/grants")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminGrants() {
        return ResponseEntity.ok(sportsOsService.grants());
    }

    @PostMapping("/admin/sports-os/grants/{grantId}/review")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> reviewGrant(@PathVariable Long grantId,
                                                           @RequestBody SportsOsRequests.GrantReviewRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.reviewGrant(grantId, request, actor(authentication)));
    }

    @PostMapping("/admin/sports-os/grants/{grantId}/disburse")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> disburseGrant(@PathVariable Long grantId,
                                                             @RequestBody SportsOsRequests.GrantDisbursementRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.disburseGrant(grantId, request, actor(authentication)));
    }

    @GetMapping("/admin/sports-os/grant-disbursements")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> grantDisbursements() {
        return ResponseEntity.ok(sportsOsService.grantDisbursements());
    }

    @GetMapping("/admin/sports-os/athletes")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> athletes() {
        return ResponseEntity.ok(sportsOsService.athletes());
    }

    @GetMapping("/admin/sports-os/volunteers")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> volunteers() {
        return ResponseEntity.ok(sportsOsService.volunteers());
    }

    @PostMapping("/admin/sports-os/volunteers/{volunteerId}/assignments")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> assignVolunteer(@PathVariable Long volunteerId,
                                                               @RequestBody SportsOsRequests.VolunteerAssignmentRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.assignVolunteer(volunteerId, request, actor(authentication)));
    }

    @GetMapping("/admin/sports-os/inspections")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> inspections() {
        return ResponseEntity.ok(sportsOsService.inspections());
    }

    @PostMapping("/admin/sports-os/inspections")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> scheduleInspection(@RequestBody SportsOsRequests.InspectionRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.scheduleInspection(request, actor(authentication)));
    }

    @PostMapping("/admin/sports-os/inspections/{inspectionId}/complete")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<Map<String, Object>> completeInspection(@PathVariable Long inspectionId,
                                                                  @RequestBody(required = false) SportsOsRequests.InspectionRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(sportsOsService.completeInspection(inspectionId, request, actor(authentication)));
    }

    @GetMapping("/admin/sports-os/audit")
    @PreAuthorize(ADMIN_SPORTS_OS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audit() {
        return ResponseEntity.ok(sportsOsService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
