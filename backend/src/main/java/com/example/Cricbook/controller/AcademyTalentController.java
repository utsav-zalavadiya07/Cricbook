package com.example.Cricbook.controller;

import com.example.Cricbook.dto.AcademyTalentRequests;
import com.example.Cricbook.service.AcademyTalentService;
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
public class AcademyTalentController {

    private static final String ADMIN_ACADEMY_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_ACADEMY_READ') or hasAuthority('ADMIN_ACADEMY_FULL') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final AcademyTalentService academyTalentService;

    @PostMapping("/academies/register")
    public ResponseEntity<Map<String, Object>> registerAcademy(@RequestBody AcademyTalentRequests.AcademyRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.registerAcademy(request, actor(authentication)));
    }

    @GetMapping("/academies/marketplace")
    public ResponseEntity<List<Map<String, Object>>> academyMarketplace(@RequestParam(required = false) String city,
                                                                        @RequestParam(required = false) String sport) {
        return ResponseEntity.ok(academyTalentService.academyMarketplace(city, sport));
    }

    @PostMapping("/coaches/register")
    public ResponseEntity<Map<String, Object>> registerCoach(@RequestBody AcademyTalentRequests.CoachRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.registerCoach(request, actor(authentication)));
    }

    @GetMapping("/coaches/marketplace")
    public ResponseEntity<List<Map<String, Object>>> coachMarketplace(@RequestParam(required = false) String sport,
                                                                      @RequestParam(required = false) String city,
                                                                      @RequestParam(required = false) Double minRating) {
        return ResponseEntity.ok(academyTalentService.coachMarketplace(sport, city, minRating));
    }

    @GetMapping("/training/programs")
    public ResponseEntity<List<Map<String, Object>>> programs(@RequestParam(required = false) String sport,
                                                              @RequestParam(required = false) String city) {
        return ResponseEntity.ok(academyTalentService.programs(sport, city));
    }

    @GetMapping("/talent/discovery")
    public ResponseEntity<List<Map<String, Object>>> talentDiscovery(@RequestParam(required = false) String city,
                                                                     @RequestParam(required = false) String sport,
                                                                     @RequestParam(required = false) String ageGroup) {
        return ResponseEntity.ok(academyTalentService.talentDiscovery(city, sport, ageGroup));
    }

    @GetMapping("/certificates/{code}/verify")
    public ResponseEntity<Map<String, Object>> verifyCertificate(@PathVariable String code) {
        return ResponseEntity.ok(academyTalentService.verifyCertificate(code));
    }

    @GetMapping("/admin/academy/dashboard")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(academyTalentService.dashboard());
    }

    @GetMapping("/admin/academy/academies")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> academies() {
        return ResponseEntity.ok(academyTalentService.academies());
    }

    @PostMapping("/admin/academy/academies")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createAcademy(@RequestBody AcademyTalentRequests.AcademyRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.registerAcademy(request, actor(authentication)));
    }

    @PostMapping("/admin/academy/academies/{id}/status")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> updateAcademyStatus(@PathVariable Long id,
                                                                   @RequestBody AcademyTalentRequests.StatusRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.updateAcademyStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/academy/coaches")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> coaches() {
        return ResponseEntity.ok(academyTalentService.coaches());
    }

    @PostMapping("/admin/academy/coaches")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createCoach(@RequestBody AcademyTalentRequests.CoachRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.registerCoach(request, actor(authentication)));
    }

    @PostMapping("/admin/academy/coaches/{id}/status")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> updateCoachStatus(@PathVariable Long id,
                                                                 @RequestBody AcademyTalentRequests.StatusRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.updateCoachStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/academy/programs")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminPrograms() {
        return ResponseEntity.ok(academyTalentService.adminPrograms());
    }

    @PostMapping("/admin/academy/programs")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createProgram(@RequestBody AcademyTalentRequests.ProgramRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.createProgram(request, actor(authentication)));
    }

    @PostMapping("/admin/academy/programs/{id}/status")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> updateProgramStatus(@PathVariable Long id,
                                                                   @RequestBody AcademyTalentRequests.StatusRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.updateProgramStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/academy/batches")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> batches() {
        return ResponseEntity.ok(academyTalentService.batches());
    }

    @PostMapping("/admin/academy/batches")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createBatch(@RequestBody AcademyTalentRequests.BatchRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.createBatch(request, actor(authentication)));
    }

    @GetMapping("/admin/academy/enrollments")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> enrollments() {
        return ResponseEntity.ok(academyTalentService.enrollments());
    }

    @PostMapping("/admin/academy/enrollments")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> enrollStudent(@RequestBody AcademyTalentRequests.EnrollmentRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.enrollStudent(request, actor(authentication)));
    }

    @PostMapping("/admin/academy/enrollments/{id}/cancel")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> cancelEnrollment(@PathVariable Long id,
                                                                Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.cancelEnrollment(id, actor(authentication)));
    }

    @GetMapping("/admin/academy/attendance")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> attendance() {
        return ResponseEntity.ok(academyTalentService.attendance());
    }

    @PostMapping("/admin/academy/attendance")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> markAttendance(@RequestBody AcademyTalentRequests.AttendanceRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.markAttendance(request, actor(authentication)));
    }

    @GetMapping("/admin/academy/assessments")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> assessments() {
        return ResponseEntity.ok(academyTalentService.assessments());
    }

    @PostMapping("/admin/academy/assessments")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> createAssessment(@RequestBody AcademyTalentRequests.AssessmentRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.createAssessment(request, actor(authentication)));
    }

    @GetMapping("/admin/academy/videos")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> videos() {
        return ResponseEntity.ok(academyTalentService.videos());
    }

    @PostMapping("/admin/academy/videos")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> addVideo(@RequestBody AcademyTalentRequests.VideoRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.addVideoAnalysis(request, actor(authentication)));
    }

    @GetMapping("/admin/academy/talent")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> talent() {
        return ResponseEntity.ok(academyTalentService.talentProfiles());
    }

    @PostMapping("/admin/academy/talent")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> publishTalent(@RequestBody AcademyTalentRequests.TalentProfileRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.publishTalentProfile(request, actor(authentication)));
    }

    @GetMapping("/admin/academy/certificates")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> certificates() {
        return ResponseEntity.ok(academyTalentService.certificates());
    }

    @PostMapping("/admin/academy/certificates")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> generateCertificate(@RequestBody AcademyTalentRequests.CertificateRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(academyTalentService.generateCertificate(request, actor(authentication)));
    }

    @GetMapping("/admin/academy/analytics")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(academyTalentService.analytics());
    }

    @GetMapping("/admin/academy/revenue")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<Map<String, Object>> revenue() {
        return ResponseEntity.ok(academyTalentService.revenueReport());
    }

    @GetMapping("/admin/academy/audit")
    @PreAuthorize(ADMIN_ACADEMY_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audit() {
        return ResponseEntity.ok(academyTalentService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
