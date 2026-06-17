package com.example.Cricbook.controller;

import com.example.Cricbook.dto.MultiSportRequests;
import com.example.Cricbook.service.MultiSportService;
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
public class MultiSportExpansionController {

    private static final String ADMIN_SPORTS_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_SPORTS_READ') or hasAuthority('ADMIN_SPORTS_FULL') or hasAuthority('SYSTEM_CONFIG_EDIT') or hasAuthority('ANALYTICS_VIEW')";

    private final MultiSportService multiSportService;

    @GetMapping("/sports/catalog")
    public ResponseEntity<List<Map<String, Object>>> catalog(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(multiSportService.catalog(activeOnly));
    }

    @GetMapping("/sports/active")
    public ResponseEntity<List<Map<String, Object>>> activeSports() {
        return ResponseEntity.ok(multiSportService.catalog(true));
    }

    @GetMapping("/sports/{code}/rules")
    public ResponseEntity<Map<String, Object>> rules(@PathVariable String code) {
        return ResponseEntity.ok(multiSportService.rules(code));
    }

    @PostMapping("/sports/rules/simulate")
    public ResponseEntity<Map<String, Object>> simulateRules(@RequestBody MultiSportRequests.SportRuleSimulationRequest request) {
        return ResponseEntity.ok(multiSportService.simulateRules(request));
    }

    @PostMapping("/sports/validate-booking")
    public ResponseEntity<Map<String, Object>> validateBooking(@RequestBody MultiSportRequests.SportBookingValidationRequest request) {
        return ResponseEntity.ok(multiSportService.validateBooking(request));
    }

    @PostMapping("/sports/validate-team")
    public ResponseEntity<Map<String, Object>> validateTeam(@RequestBody MultiSportRequests.SportTeamValidationRequest request) {
        return ResponseEntity.ok(multiSportService.validateTeam(request));
    }

    @GetMapping("/admin/sports/dashboard")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(multiSportService.dashboard());
    }

    @GetMapping("/admin/sports/catalog")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminCatalog() {
        return ResponseEntity.ok(multiSportService.catalog(false));
    }

    @PostMapping("/admin/sports")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> createSport(@RequestBody MultiSportRequests.SportConfigRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(multiSportService.createSport(request, actor(authentication)));
    }

    @PutMapping("/admin/sports/{id}")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> updateSport(@PathVariable Long id,
                                                           @RequestBody MultiSportRequests.SportConfigRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(multiSportService.updateSport(id, request, actor(authentication)));
    }

    @PatchMapping("/admin/sports/{id}/status")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> setSportStatus(@PathVariable Long id,
                                                              @RequestBody MultiSportRequests.SportStatusRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(multiSportService.setSportStatus(id, request, actor(authentication)));
    }

    @PostMapping("/admin/sports/turfs")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> mapSportToTurf(@RequestBody MultiSportRequests.SportTurfMappingRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(multiSportService.mapSportToTurf(request, actor(authentication)));
    }

    @DeleteMapping("/admin/sports/{sportId}/turfs/{turfId}")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> removeSportFromTurf(@PathVariable Long sportId,
                                                                   @PathVariable Long turfId,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(multiSportService.removeSportFromTurf(sportId, turfId, actor(authentication)));
    }

    @GetMapping("/admin/sports/analytics")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(multiSportService.analytics());
    }

    @GetMapping("/admin/sports/audit")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audits() {
        return ResponseEntity.ok(multiSportService.audits());
    }

    @PostMapping("/admin/sports/seed-defaults")
    @PreAuthorize(ADMIN_SPORTS_ACCESS)
    public ResponseEntity<Map<String, Object>> seedDefaults(Authentication authentication) {
        return ResponseEntity.ok(multiSportService.seedDefaults(actor(authentication)));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
