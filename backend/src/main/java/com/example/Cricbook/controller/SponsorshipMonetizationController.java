package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SponsorshipRequests;
import com.example.Cricbook.service.SponsorshipMonetizationService;
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
public class SponsorshipMonetizationController {

    private static final String ADMIN_MONETIZATION_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_MONETIZATION_READ') or hasAuthority('ADMIN_MONETIZATION_FULL') or hasAuthority('CAMPAIGN_CREATE') or hasAuthority('REPORT_VIEW')";

    private final SponsorshipMonetizationService sponsorshipMonetizationService;

    @GetMapping("/sponsorship/marketplace")
    public ResponseEntity<List<Map<String, Object>>> marketplace(@RequestParam(required = false) String city,
                                                                  @RequestParam(required = false) String sportCategory,
                                                                  @RequestParam(required = false) BigDecimal maxAmount) {
        return ResponseEntity.ok(sponsorshipMonetizationService.marketplace(city, sportCategory, maxAmount));
    }

    @PostMapping("/sponsorship/brands")
    public ResponseEntity<Map<String, Object>> registerBrand(@RequestBody SponsorshipRequests.BrandRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.registerBrand(request, actor(authentication)));
    }

    @PostMapping("/sponsorship/opportunities/{id}/proposals")
    public ResponseEntity<Map<String, Object>> submitProposal(@PathVariable Long id,
                                                              @RequestBody SponsorshipRequests.ProposalRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.submitProposal(id, request, actor(authentication)));
    }

    @PostMapping("/sponsorship/campaigns/{id}/track")
    public ResponseEntity<Map<String, Object>> trackAdEvent(@PathVariable Long id,
                                                            @RequestBody SponsorshipRequests.EventRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.recordAdEvent(id, request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/dashboard")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(sponsorshipMonetizationService.dashboard());
    }

    @GetMapping("/admin/monetization/brands")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> brands() {
        return ResponseEntity.ok(sponsorshipMonetizationService.brands());
    }

    @PostMapping("/admin/monetization/brands")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> createBrand(@RequestBody SponsorshipRequests.BrandRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.registerBrand(request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/opportunities")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> opportunities() {
        return ResponseEntity.ok(sponsorshipMonetizationService.opportunities());
    }

    @PostMapping("/admin/monetization/opportunities")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> createOpportunity(@RequestBody SponsorshipRequests.OpportunityRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.createOpportunity(request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/proposals")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> proposals() {
        return ResponseEntity.ok(sponsorshipMonetizationService.proposals());
    }

    @PostMapping("/admin/monetization/proposals/{id}/status")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> updateProposalStatus(@PathVariable Long id,
                                                                    @RequestBody SponsorshipRequests.StatusRequest request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.updateProposalStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/campaigns")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> campaigns() {
        return ResponseEntity.ok(sponsorshipMonetizationService.campaigns());
    }

    @PostMapping("/admin/monetization/campaigns")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody SponsorshipRequests.CampaignRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.createCampaign(request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/leads")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> leads() {
        return ResponseEntity.ok(sponsorshipMonetizationService.leads());
    }

    @PostMapping("/admin/monetization/leads")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> createLead(@RequestBody SponsorshipRequests.LeadRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.createLead(request, actor(authentication)));
    }

    @PostMapping("/admin/monetization/leads/{id}/status")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> updateLeadStatus(@PathVariable Long id,
                                                                @RequestBody SponsorshipRequests.StatusRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.updateLeadStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/contracts")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> contracts() {
        return ResponseEntity.ok(sponsorshipMonetizationService.contracts());
    }

    @PostMapping("/admin/monetization/contracts")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> createContract(@RequestBody SponsorshipRequests.ContractRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.createContract(request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/invoices")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> invoices() {
        return ResponseEntity.ok(sponsorshipMonetizationService.invoices());
    }

    @PostMapping("/admin/monetization/invoices")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> createInvoice(@RequestBody SponsorshipRequests.InvoiceRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(sponsorshipMonetizationService.createInvoice(request, actor(authentication)));
    }

    @GetMapping("/admin/monetization/reports")
    @PreAuthorize(ADMIN_MONETIZATION_ACCESS)
    public ResponseEntity<Map<String, Object>> reports() {
        return ResponseEntity.ok(sponsorshipMonetizationService.reports());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
