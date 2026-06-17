package com.example.Cricbook.controller;

import com.example.Cricbook.dto.DeveloperPlatformRequests;
import com.example.Cricbook.service.DeveloperPlatformService;
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
public class DeveloperPlatformController {

    private static final String ADMIN_DEVELOPER_ACCESS = "hasRole('SUPER_ADMIN') or hasRole('ADMIN') or hasAuthority('ADMIN_DEVELOPERS_READ') or hasAuthority('ADMIN_DEVELOPERS_FULL') or hasAuthority('SYSTEM_CONFIG_EDIT') or hasAuthority('ANALYTICS_VIEW')";

    private final DeveloperPlatformService developerPlatformService;

    @PostMapping("/developers/register")
    public ResponseEntity<Map<String, Object>> registerDeveloper(@RequestBody DeveloperPlatformRequests.DeveloperRegistrationRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.registerDeveloper(request, actor(authentication)));
    }

    @GetMapping("/developer-platform/docs")
    public ResponseEntity<Map<String, Object>> docs() {
        return ResponseEntity.ok(developerPlatformService.docs());
    }

    @GetMapping("/developer-platform/marketplace")
    public ResponseEntity<List<Map<String, Object>>> marketplace() {
        return ResponseEntity.ok(developerPlatformService.publicMarketplace());
    }

    @PostMapping("/developer-platform/oauth/token")
    public ResponseEntity<Map<String, Object>> oauthToken(@RequestBody DeveloperPlatformRequests.OAuthTokenRequest request) {
        return ResponseEntity.ok(developerPlatformService.issueOAuthToken(request));
    }

    @GetMapping("/platform-api/v1/turfs/search")
    public ResponseEntity<Map<String, Object>> gatewayTurfSearch(@RequestHeader("X-Truf-Api-Key") String apiKey,
                                                                 @RequestParam(required = false) String query,
                                                                 @RequestParam(required = false) String city,
                                                                 @RequestParam(required = false) String sport) {
        return ResponseEntity.ok(developerPlatformService.gatewaySearchTurfs(apiKey, query, city, sport));
    }

    @GetMapping("/admin/developer-platform/dashboard")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(developerPlatformService.dashboard());
    }

    @GetMapping("/admin/developer-platform/developers")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> developers() {
        return ResponseEntity.ok(developerPlatformService.developers());
    }

    @PostMapping("/admin/developer-platform/developers")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> createDeveloper(@RequestBody DeveloperPlatformRequests.DeveloperRegistrationRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.registerDeveloper(request, actor(authentication)));
    }

    @PatchMapping("/admin/developer-platform/developers/{id}/status")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> updateDeveloperStatus(@PathVariable Long id,
                                                                     @RequestBody DeveloperPlatformRequests.DeveloperStatusRequest request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.updateDeveloperStatus(id, request, actor(authentication)));
    }

    @GetMapping("/admin/developer-platform/applications")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> applications() {
        return ResponseEntity.ok(developerPlatformService.applications());
    }

    @PostMapping("/admin/developer-platform/applications")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> createApplication(@RequestBody DeveloperPlatformRequests.ApplicationRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.createApplication(request, actor(authentication)));
    }

    @PostMapping("/admin/developer-platform/applications/sandbox")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> createSandbox(@RequestBody Map<String, Object> request,
                                                             Authentication authentication) {
        Object rawDeveloperId = request == null ? null : request.get("developerId");
        Long developerId = rawDeveloperId instanceof Number number ? number.longValue() : rawDeveloperId == null ? null : Long.valueOf(String.valueOf(rawDeveloperId));
        return ResponseEntity.ok(developerPlatformService.createSandbox(developerId, actor(authentication)));
    }

    @GetMapping("/admin/developer-platform/api-keys")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> apiKeys() {
        return ResponseEntity.ok(developerPlatformService.apiKeys());
    }

    @PostMapping("/admin/developer-platform/api-keys")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> generateApiKey(@RequestBody DeveloperPlatformRequests.ApiKeyRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.generateApiKey(request, actor(authentication)));
    }

    @PostMapping("/admin/developer-platform/api-keys/{id}/rotate")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> rotateApiKey(@PathVariable Long id,
                                                            Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.rotateApiKey(id, actor(authentication)));
    }

    @PostMapping("/admin/developer-platform/api-keys/{id}/revoke")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> revokeApiKey(@PathVariable Long id,
                                                            Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.revokeApiKey(id, actor(authentication)));
    }

    @GetMapping("/admin/developer-platform/webhooks")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> webhooks() {
        return ResponseEntity.ok(developerPlatformService.webhooks());
    }

    @PostMapping("/admin/developer-platform/webhooks")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> createWebhook(@RequestBody DeveloperPlatformRequests.WebhookRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.createWebhook(request, actor(authentication)));
    }

    @PostMapping("/admin/developer-platform/webhooks/{id}/test")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable Long id,
                                                           @RequestParam(defaultValue = "booking.created") String eventType,
                                                           Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.testWebhook(id, eventType, actor(authentication)));
    }

    @GetMapping("/admin/developer-platform/analytics")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> analytics() {
        return ResponseEntity.ok(developerPlatformService.analytics());
    }

    @GetMapping("/admin/developer-platform/billing")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> billing() {
        return ResponseEntity.ok(developerPlatformService.billing());
    }

    @GetMapping("/admin/developer-platform/audit")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> audit() {
        return ResponseEntity.ok(developerPlatformService.audits());
    }

    @GetMapping("/admin/developer-platform/marketplace")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<List<Map<String, Object>>> adminMarketplace() {
        return ResponseEntity.ok(developerPlatformService.marketplaceListings());
    }

    @PostMapping("/admin/developer-platform/marketplace")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> createMarketplaceListing(@RequestBody DeveloperPlatformRequests.MarketplaceListingRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.createMarketplaceListing(request, actor(authentication)));
    }

    @PostMapping("/admin/developer-platform/marketplace/{id}/status")
    @PreAuthorize(ADMIN_DEVELOPER_ACCESS)
    public ResponseEntity<Map<String, Object>> updateMarketplaceStatus(@PathVariable Long id,
                                                                       @RequestBody DeveloperPlatformRequests.StatusRequest request,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(developerPlatformService.updateMarketplaceStatus(id, request, actor(authentication)));
    }

    private String actor(Authentication authentication) {
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
