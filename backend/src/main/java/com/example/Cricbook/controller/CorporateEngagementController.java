package com.example.Cricbook.controller;

import com.example.Cricbook.dto.CorporateEngagementRequests;
import com.example.Cricbook.service.CorporateEngagementService;
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
public class CorporateEngagementController {

    private static final String ADMIN_CORPORATE_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_CORPORATE_READ') or hasAuthority('ADMIN_CORPORATE_WRITE') or hasAuthority('ADMIN_CORPORATE_FULL') or hasAuthority('ANALYTICS_VIEW') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final CorporateEngagementService corporateEngagementService;

    @PostMapping("/corporates/register")
    public ResponseEntity<Map<String, Object>> registerCompany(@RequestBody CorporateEngagementRequests.CompanyRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.registerCompany(request, actor(authentication)));
    }

    @GetMapping("/corporates/marketplace")
    public ResponseEntity<List<Map<String, Object>>> marketplace(@RequestParam(required = false) String city) {
        return ResponseEntity.ok(corporateEngagementService.marketplace(city));
    }

    @GetMapping("/admin/corporate/dashboard")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(corporateEngagementService.dashboard());
    }

    @GetMapping("/admin/corporate/companies")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> companies() {
        return ResponseEntity.ok(corporateEngagementService.companies());
    }

    @PostMapping("/admin/corporate/companies")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createCompany(@RequestBody CorporateEngagementRequests.CompanyRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.registerCompany(request, actor(authentication)));
    }

    @PostMapping("/admin/corporate/companies/{id}/status")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateCompanyStatus(@PathVariable Long id,
                                                                   @RequestBody CorporateEngagementRequests.StatusRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.updateCompanyStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/locations")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> locations() {
        return ResponseEntity.ok(corporateEngagementService.locations());
    }

    @PostMapping("/admin/corporate/locations")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createLocation(@RequestBody CorporateEngagementRequests.LocationRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createLocation(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/departments")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> departments() {
        return ResponseEntity.ok(corporateEngagementService.departments());
    }

    @PostMapping("/admin/corporate/departments")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createDepartment(@RequestBody CorporateEngagementRequests.DepartmentRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createDepartment(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/employees")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> employees() {
        return ResponseEntity.ok(corporateEngagementService.employees());
    }

    @PostMapping("/admin/corporate/employees")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createEmployee(@RequestBody CorporateEngagementRequests.EmployeeRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createEmployee(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/teams")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> teams() {
        return ResponseEntity.ok(corporateEngagementService.teams());
    }

    @PostMapping("/admin/corporate/teams")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createTeam(@RequestBody CorporateEngagementRequests.TeamRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createTeam(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/team-memberships")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> memberships() {
        return ResponseEntity.ok(corporateEngagementService.memberships());
    }

    @PostMapping("/admin/corporate/team-memberships")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> addTeamMember(@RequestBody CorporateEngagementRequests.TeamMembershipRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.addTeamMember(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/booking-plans")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> bookingPlans() {
        return ResponseEntity.ok(corporateEngagementService.bookingPlans());
    }

    @PostMapping("/admin/corporate/booking-plans")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createBookingPlan(@RequestBody CorporateEngagementRequests.BookingPlanRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createBookingPlan(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/tournaments")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> tournaments() {
        return ResponseEntity.ok(corporateEngagementService.tournaments());
    }

    @PostMapping("/admin/corporate/tournaments")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createTournament(@RequestBody CorporateEngagementRequests.TournamentRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createTournament(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/wellness-programs")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> wellnessPrograms() {
        return ResponseEntity.ok(corporateEngagementService.wellnessPrograms());
    }

    @PostMapping("/admin/corporate/wellness-programs")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createWellnessProgram(@RequestBody CorporateEngagementRequests.WellnessProgramRequest request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createWellnessProgram(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/wellness-activities")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> wellnessActivities() {
        return ResponseEntity.ok(corporateEngagementService.wellnessActivities());
    }

    @PostMapping("/admin/corporate/wellness-activities")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> recordWellnessActivity(@RequestBody CorporateEngagementRequests.WellnessActivityRequest request,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.recordWellnessActivity(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/rewards")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> rewards() {
        return ResponseEntity.ok(corporateEngagementService.rewards());
    }

    @PostMapping("/admin/corporate/rewards")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> creditReward(@RequestBody CorporateEngagementRequests.RewardRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.creditReward(request, actor(authentication)));
    }

    @PostMapping("/admin/corporate/rewards/redeem")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> redeemReward(@RequestBody CorporateEngagementRequests.RewardRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.redeemReward(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/invoices")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> invoices() {
        return ResponseEntity.ok(corporateEngagementService.invoices());
    }

    @PostMapping("/admin/corporate/invoices")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> generateInvoice(@RequestBody CorporateEngagementRequests.InvoiceRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.generateInvoice(request, actor(authentication)));
    }

    @PostMapping("/admin/corporate/invoices/{id}/payment")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> updateInvoicePayment(@PathVariable Long id,
                                                                    @RequestBody CorporateEngagementRequests.PaymentRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.updateInvoicePayment(id, request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/contracts")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> contracts() {
        return ResponseEntity.ok(corporateEngagementService.contracts());
    }

    @PostMapping("/admin/corporate/contracts")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody CorporateEngagementRequests.ContractRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createContract(request, actor(authentication)));
    }

    @PostMapping("/admin/corporate/contracts/{id}/renew")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> renewContract(@PathVariable Long id,
                                                             @RequestBody(required = false) CorporateEngagementRequests.ContractRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.renewContract(id, request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/leagues")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> leagues() {
        return ResponseEntity.ok(corporateEngagementService.leagues());
    }

    @PostMapping("/admin/corporate/leagues")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> createLeague(@RequestBody CorporateEngagementRequests.LeagueRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(corporateEngagementService.createLeague(request, actor(authentication)));
    }

    @GetMapping("/admin/corporate/analytics")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(corporateEngagementService.analytics());
    }

    @GetMapping("/admin/corporate/billing")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<Map<String, Object>> billing() {
        return ResponseEntity.ok(corporateEngagementService.billing());
    }

    @GetMapping("/admin/corporate/audit")
    @PreAuthorize(ADMIN_CORPORATE_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audits() {
        return ResponseEntity.ok(corporateEngagementService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "public" : authentication.getName();
    }
}
