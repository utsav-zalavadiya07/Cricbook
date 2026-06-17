package com.example.Cricbook.controller;

import com.example.Cricbook.dto.SaasPlatformRequests;
import com.example.Cricbook.service.SaasPlatformService;
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
public class SaasPlatformController {

    private static final String ADMIN_SAAS_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('SAAS_PLATFORM_ADMIN') or hasAuthority('ADMIN_GOVERNANCE_FULL') or hasAuthority('SYSTEM_CONFIG_EDIT')";

    private final SaasPlatformService saasPlatformService;

    @GetMapping("/saas/tenant-context")
    public ResponseEntity<Map<String, Object>> tenantContext(@RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(saasPlatformService.publicTenantContext(tenantId));
    }

    @GetMapping("/saas/marketplace/modules")
    public ResponseEntity<List<Map<String, Object>>> marketplaceModules() {
        return ResponseEntity.ok(saasPlatformService.marketplaceModules());
    }

    @GetMapping("/saas/regions")
    public ResponseEntity<List<Map<String, Object>>> regions() {
        return ResponseEntity.ok(saasPlatformService.regions());
    }

    @GetMapping("/admin/saas/dashboard")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(saasPlatformService.adminDashboard());
    }

    @GetMapping("/admin/saas/tenants")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> tenants() {
        return ResponseEntity.ok(saasPlatformService.tenants());
    }

    @PostMapping("/admin/saas/tenants")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> createTenant(@RequestBody SaasPlatformRequests.TenantRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.createTenant(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/tenants/{tenantId}/status")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> updateTenantStatus(@PathVariable Long tenantId,
                                                                  @RequestBody SaasPlatformRequests.TenantStatusRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.updateTenantStatus(tenantId, request, actor(authentication)));
    }

    @PostMapping("/admin/saas/branding")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> saveBranding(@RequestBody SaasPlatformRequests.BrandingRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.saveBranding(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/domains")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> addDomain(@RequestBody SaasPlatformRequests.CustomDomainRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.addDomain(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/domains/{domainId}/verify")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> verifyDomain(@PathVariable Long domainId,
                                                            Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.verifyDomain(domainId, actor(authentication)));
    }

    @PostMapping("/admin/saas/domains/{domainId}/activate")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> activateDomain(@PathVariable Long domainId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.activateDomain(domainId, actor(authentication)));
    }

    @PostMapping("/admin/saas/features")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> setFeatureToggle(@RequestBody SaasPlatformRequests.FeatureToggleRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.setFeatureToggle(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/subscriptions")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> createSubscription(@RequestBody SaasPlatformRequests.SubscriptionRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.createSubscription(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/subscriptions/{subscriptionId}/renew")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> renewSubscription(@PathVariable Long subscriptionId,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.renewSubscription(subscriptionId, actor(authentication)));
    }

    @PostMapping("/admin/saas/invoices")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> generateInvoice(@RequestBody SaasPlatformRequests.InvoiceRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.generateInvoice(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/invoices/{invoiceId}/pay")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> payInvoice(@PathVariable Long invoiceId,
                                                          @RequestBody SaasPlatformRequests.InvoicePaymentRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.payInvoice(invoiceId, request, actor(authentication)));
    }

    @PostMapping("/admin/saas/marketplace/modules")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> publishMarketplaceModule(@RequestBody SaasPlatformRequests.MarketplaceModuleRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.publishMarketplaceModule(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/marketplace/installations")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> installModule(@RequestBody SaasPlatformRequests.ModuleInstallationRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.installModule(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/app-builds")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> requestAppBuild(@RequestBody SaasPlatformRequests.AppBuildRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.requestAppBuild(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/compliance")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> createComplianceRequest(@RequestBody SaasPlatformRequests.ComplianceRequest request,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.createComplianceRequest(request, actor(authentication)));
    }

    @PostMapping("/admin/saas/resellers")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<Map<String, Object>> registerReseller(@RequestBody SaasPlatformRequests.ResellerRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(saasPlatformService.registerReseller(request, actor(authentication)));
    }

    @GetMapping("/admin/saas/audit")
    @PreAuthorize(ADMIN_SAAS_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audit() {
        return ResponseEntity.ok(saasPlatformService.audits());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
