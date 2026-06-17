package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CareerPlatformRequests;
import com.example.Cricbook.service.CareerPlatformService;
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
public class CareerPlatformController {

    private static final String ADMIN_CAREER_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_CAREER_READ') or hasAuthority('ADMIN_CAREER_FULL') or hasAuthority('ADMIN_ACADEMY_READ') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final CareerPlatformService careerPlatformService;

    @GetMapping("/career/marketplace")
    public ResponseEntity<List<Map<String, Object>>> talentMarketplace(@RequestParam(required = false) String city,
                                                                       @RequestParam(required = false) String sport,
                                                                       @RequestParam(required = false) String ageGroup,
                                                                       @RequestParam(required = false) Integer minScore) {
        return ResponseEntity.ok(careerPlatformService.talentMarketplace(city, sport, ageGroup, minScore));
    }

    @GetMapping("/career/opportunities")
    public ResponseEntity<List<Map<String, Object>>> opportunities(@RequestParam(required = false) String type,
                                                                   @RequestParam(required = false) String sport,
                                                                   @RequestParam(required = false) String location) {
        return ResponseEntity.ok(careerPlatformService.opportunities(type, sport, location));
    }

    @GetMapping("/career/resumes/{shareCode}")
    public ResponseEntity<Map<String, Object>> publicResume(@PathVariable String shareCode) {
        return ResponseEntity.ok(careerPlatformService.publicResume(shareCode));
    }

    @PostMapping("/career/scouts/register")
    public ResponseEntity<Map<String, Object>> registerScout(@RequestBody CareerPlatformRequests.ScoutRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.registerScout(request, actor(authentication)));
    }

    @PostMapping("/career/profiles")
    public ResponseEntity<Map<String, Object>> createProfile(@RequestBody CareerPlatformRequests.ProfileRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.createProfile(request, actor(authentication)));
    }

    @PutMapping("/career/profiles/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(@PathVariable Long id,
                                                             @RequestBody CareerPlatformRequests.ProfileRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.updateProfile(id, request, actor(authentication)));
    }

    @PostMapping("/career/profiles/{id}/resume")
    public ResponseEntity<Map<String, Object>> generateResume(@PathVariable Long id,
                                                              Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.generateResume(id, actor(authentication)));
    }

    @PostMapping("/career/profiles/{id}/achievements")
    public ResponseEntity<Map<String, Object>> addAchievement(@PathVariable Long id,
                                                              @RequestBody CareerPlatformRequests.AchievementRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.addAchievement(id, request, actor(authentication)));
    }

    @PostMapping("/career/profiles/{id}/timeline")
    public ResponseEntity<Map<String, Object>> addTimelineEvent(@PathVariable Long id,
                                                                @RequestBody CareerPlatformRequests.TimelineRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.addTimelineEvent(id, request, actor(authentication)));
    }

    @PostMapping("/career/profiles/{id}/videos")
    public ResponseEntity<Map<String, Object>> addHighlightVideo(@PathVariable Long id,
                                                                 @RequestBody CareerPlatformRequests.VideoRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.addHighlightVideo(id, request, actor(authentication)));
    }

    @PostMapping("/career/opportunities/{id}/applications")
    public ResponseEntity<Map<String, Object>> applyForOpportunity(@PathVariable Long id,
                                                                   @RequestBody CareerPlatformRequests.ApplicationRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.applyForOpportunity(id, request, actor(authentication)));
    }

    @PostMapping("/career/recommendations")
    public ResponseEntity<Map<String, Object>> createRecommendation(@RequestBody CareerPlatformRequests.RecommendationRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.createRecommendation(request, actor(authentication)));
    }

    @GetMapping("/admin/career/dashboard")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(careerPlatformService.dashboard());
    }

    @GetMapping("/admin/career/profiles")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> profiles() {
        return ResponseEntity.ok(careerPlatformService.profiles());
    }

    @GetMapping("/admin/career/scouts")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> scouts() {
        return ResponseEntity.ok(careerPlatformService.scouts());
    }

    @PostMapping("/admin/career/scouts/{id}/status")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> updateScoutStatus(@PathVariable Long id,
                                                                 @RequestBody CareerPlatformRequests.StatusRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.updateScoutStatus(id, request, actor(authentication)));
    }

    @PostMapping("/admin/career/scouts/{id}/activities")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> recordScoutActivity(@PathVariable Long id,
                                                                   @RequestBody CareerPlatformRequests.ScoutActivityRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.recordScoutActivity(id, request, actor(authentication)));
    }

    @GetMapping("/admin/career/opportunities")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminOpportunities(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(careerPlatformService.adminOpportunities(type));
    }

    @PostMapping("/admin/career/opportunities")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> createOpportunity(@RequestBody CareerPlatformRequests.OpportunityRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.createOpportunity(request, actor(authentication)));
    }

    @GetMapping("/admin/career/applications")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> applications() {
        return ResponseEntity.ok(careerPlatformService.applications());
    }

    @PostMapping("/admin/career/applications/{id}/status")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(@PathVariable Long id,
                                                                       @RequestBody CareerPlatformRequests.StatusRequest request,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.updateApplicationStatus(id, request, actor(authentication)));
    }

    @PostMapping("/admin/career/applications/{id}/evaluations")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> evaluateApplication(@PathVariable Long id,
                                                                   @RequestBody CareerPlatformRequests.EvaluationRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.evaluateApplication(id, request, actor(authentication)));
    }

    @GetMapping("/admin/career/evaluations")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> evaluations() {
        return ResponseEntity.ok(careerPlatformService.evaluations());
    }

    @PostMapping("/admin/career/achievements/{id}/verify")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> verifyAchievement(@PathVariable Long id,
                                                                 @RequestBody CareerPlatformRequests.StatusRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.verifyAchievement(id, request, actor(authentication)));
    }

    @PostMapping("/admin/career/recommendations/{id}/approve")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<Map<String, Object>> approveRecommendation(@PathVariable Long id,
                                                                     @RequestBody CareerPlatformRequests.StatusRequest request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(careerPlatformService.approveRecommendation(id, request, actor(authentication)));
    }

    @GetMapping("/admin/career/audit")
    @PreAuthorize(ADMIN_CAREER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audits() {
        return ResponseEntity.ok(careerPlatformService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
