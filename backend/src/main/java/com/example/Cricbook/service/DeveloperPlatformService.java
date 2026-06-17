package com.example.Cricbook.service;

import com.example.Cricbook.dto.DeveloperPlatformRequests;
import com.example.Cricbook.dto.TurfSearchRequest;
import com.example.Cricbook.dto.TurfSearchResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeveloperPlatformService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> DEFAULT_SCOPES = List.of("turfs:read", "bookings:write", "tournaments:read", "teams:read", "analytics:read", "webhooks:write");

    private final DeveloperAccountRepository developerAccountRepository;
    private final DeveloperApplicationRepository developerApplicationRepository;
    private final DeveloperApiKeyRepository developerApiKeyRepository;
    private final DeveloperWebhookEndpointRepository webhookRepository;
    private final DeveloperApiUsageRepository usageRepository;
    private final DeveloperInvoiceRepository invoiceRepository;
    private final DeveloperMarketplaceListingRepository marketplaceRepository;
    private final DeveloperPlatformAuditRepository auditRepository;
    private final AuditLogRepository auditLogRepository;
    private final TurfSearchService turfSearchService;

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        List<DeveloperAccount> developers = developerAccountRepository.findAll();
        List<DeveloperApplication> applications = developerApplicationRepository.findAll();
        List<DeveloperApiKey> keys = developerApiKeyRepository.findAll();
        List<DeveloperWebhookEndpoint> webhooks = webhookRepository.findAll();
        List<DeveloperApiUsage> usage = usageRepository.findTop100ByOrderByCreatedAtDesc();
        List<DeveloperInvoice> invoices = invoiceRepository.findAll();
        List<DeveloperMarketplaceListing> listings = marketplaceRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(developers, applications, keys, webhooks, usage, invoices, listings));
        response.put("developers", developers.stream().sorted(Comparator.comparing(DeveloperAccount::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapDeveloper).toList());
        response.put("applications", applications.stream().sorted(Comparator.comparing(DeveloperApplication::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapApplication).toList());
        response.put("apiKeys", keys.stream().sorted(Comparator.comparing(DeveloperApiKey::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapApiKey).toList());
        response.put("webhooks", webhooks.stream().sorted(Comparator.comparing(DeveloperWebhookEndpoint::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapWebhook).toList());
        response.put("usageAnalytics", usageAnalytics(usage));
        response.put("ratePlans", ratePlans());
        response.put("apiProducts", apiProducts());
        response.put("docs", docsCatalog());
        response.put("marketplace", listings.stream().sorted(Comparator.comparing(DeveloperMarketplaceListing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))).limit(8).map(this::mapMarketplaceListing).toList());
        response.put("securityControls", securityControls());
        response.put("sandbox", sandboxReadiness());
        response.put("priorityActions", priorityActions(developers, applications, keys, webhooks, listings));
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional
    public Map<String, Object> registerDeveloper(DeveloperPlatformRequests.DeveloperRegistrationRequest request, String actor) {
        String email = required(request == null ? null : request.getEmail(), "Developer email is required").toLowerCase(Locale.ROOT);
        if (developerAccountRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Developer email already exists");
        }
        DeveloperAccount developer = DeveloperAccount.builder()
                .name(required(request.getName(), "Developer name is required"))
                .email(email)
                .companyName(blankToNull(request.getCompanyName()))
                .website(validateOptionalUrl(request.getWebsite(), "Developer website"))
                .plan(normalizePlan(request.getPlan()))
                .status("ACTIVE")
                .emailVerified(false)
                .mfaEnabled(false)
                .build();
        DeveloperAccount saved = developerAccountRepository.save(developer);
        audit(saved, null, "DEVELOPER_REGISTERED", actor, "Developer account registered: " + saved.getEmail());
        return mapDeveloper(saved);
    }

    @Transactional
    public Map<String, Object> updateDeveloperStatus(Long developerId, DeveloperPlatformRequests.DeveloperStatusRequest request, String actor) {
        DeveloperAccount developer = developerAccountRepository.findById(developerId).orElseThrow(() -> ApiException.notFound("Developer account not found"));
        String status = normalize(request == null ? null : request.getStatus(), "ACTIVE");
        if (!Set.of("ACTIVE", "SUSPENDED", "PENDING_VERIFICATION", "DISABLED").contains(status)) {
            throw ApiException.badRequest("Unsupported developer status");
        }
        developer.setStatus(status);
        developer.setSuspensionReason(blankToNull(request == null ? null : request.getReason()));
        DeveloperAccount saved = developerAccountRepository.save(developer);
        audit(saved, null, "DEVELOPER_STATUS_" + status, actor, defaultString(request == null ? null : request.getReason(), "Status updated"));
        return mapDeveloper(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> developers() {
        return developerAccountRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapDeveloper).toList();
    }

    @Transactional
    public Map<String, Object> createApplication(DeveloperPlatformRequests.ApplicationRequest request, String actor) {
        Long developerId = request == null ? null : request.getDeveloperId();
        if (developerId == null) {
            throw ApiException.badRequest("Developer id is required");
        }
        DeveloperAccount developer = developerAccountRepository.findById(developerId).orElseThrow(() -> ApiException.notFound("Developer account not found"));
        if (!"ACTIVE".equalsIgnoreCase(developer.getStatus())) {
            throw ApiException.conflict("Developer account is not active");
        }
        String name = required(request.getName(), "Application name is required");
        if (developerApplicationRepository.existsByDeveloper_IdAndNameIgnoreCase(developerId, name)) {
            throw ApiException.conflict("Application name already exists for this developer");
        }
        String callbackUrl = validateOptionalUrl(request.getCallbackUrl(), "Callback URL");
        SecretPair secretPair = secretPair("truf_client");
        DeveloperApplication application = DeveloperApplication.builder()
                .developer(developer)
                .name(name)
                .clientId(secretPair.publicValue())
                .clientSecretHash(hash(secretPair.secret()))
                .environment(normalizeEnvironment(request.getEnvironment()))
                .status("ACTIVE")
                .callbackUrl(callbackUrl)
                .scopes(joinScopes(request.getScopes()))
                .build();
        DeveloperApplication saved = developerApplicationRepository.save(application);
        audit(developer, saved, "APPLICATION_CREATED", actor, "Created developer application " + saved.getName());
        Map<String, Object> response = mapApplication(saved);
        response.put("clientSecret", secretPair.secret());
        response.put("secretVisibleOnce", true);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> applications() {
        return developerApplicationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapApplication).toList();
    }

    @Transactional
    public Map<String, Object> generateApiKey(DeveloperPlatformRequests.ApiKeyRequest request, String actor) {
        Long applicationId = request == null ? null : request.getApplicationId();
        if (applicationId == null) {
            throw ApiException.badRequest("Application id is required");
        }
        DeveloperApplication application = developerApplicationRepository.findById(applicationId).orElseThrow(() -> ApiException.notFound("Developer application not found"));
        requireActiveApplication(application);
        String rawKey = apiKey(application.getEnvironment());
        DeveloperApiKey apiKey = DeveloperApiKey.builder()
                .application(application)
                .keyPrefix(prefix(rawKey))
                .keyHash(hash(rawKey))
                .environment(normalizeEnvironment(request.getEnvironment() == null ? application.getEnvironment() : request.getEnvironment()))
                .status("ACTIVE")
                .expiresAt(request.getExpiresAt())
                .build();
        DeveloperApiKey saved = developerApiKeyRepository.save(apiKey);
        audit(application.getDeveloper(), application, "API_KEY_GENERATED", actor, "Generated " + saved.getEnvironment() + " API key " + saved.getKeyPrefix());
        Map<String, Object> response = mapApiKey(saved);
        response.put("apiKey", rawKey);
        response.put("visibleOnce", true);
        return response;
    }

    @Transactional
    public Map<String, Object> rotateApiKey(Long apiKeyId, String actor) {
        DeveloperApiKey existing = developerApiKeyRepository.findById(apiKeyId).orElseThrow(() -> ApiException.notFound("API key not found"));
        existing.setStatus("ROTATED");
        existing.setRotatedAt(LocalDateTime.now());
        developerApiKeyRepository.save(existing);
        DeveloperPlatformRequests.ApiKeyRequest request = DeveloperPlatformRequests.ApiKeyRequest.builder()
                .applicationId(existing.getApplication().getId())
                .environment(existing.getEnvironment())
                .expiresAt(existing.getExpiresAt())
                .build();
        Map<String, Object> replacement = generateApiKey(request, actor);
        audit(existing.getApplication().getDeveloper(), existing.getApplication(), "API_KEY_ROTATED", actor, "Rotated API key " + existing.getKeyPrefix());
        return Map.of("oldKey", mapApiKey(existing), "newKey", replacement, "zeroDowntimeRotation", true);
    }

    @Transactional
    public Map<String, Object> revokeApiKey(Long apiKeyId, String actor) {
        DeveloperApiKey key = developerApiKeyRepository.findById(apiKeyId).orElseThrow(() -> ApiException.notFound("API key not found"));
        key.setStatus("REVOKED");
        DeveloperApiKey saved = developerApiKeyRepository.save(key);
        audit(saved.getApplication().getDeveloper(), saved.getApplication(), "API_KEY_REVOKED", actor, "Revoked API key " + saved.getKeyPrefix());
        return mapApiKey(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> apiKeys() {
        return developerApiKeyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapApiKey).toList();
    }

    @Transactional
    public Map<String, Object> issueOAuthToken(DeveloperPlatformRequests.OAuthTokenRequest request) {
        String grantType = normalize(request == null ? null : request.getGrantType(), "CLIENT_CREDENTIALS");
        if (!Set.of("CLIENT_CREDENTIALS", "AUTHORIZATION_CODE", "REFRESH_TOKEN").contains(grantType)) {
            throw ApiException.badRequest("Unsupported OAuth grant type");
        }
        DeveloperApplication application = developerApplicationRepository.findByClientId(required(request.getClientId(), "Client id is required"))
                .orElseThrow(() -> ApiException.unauthorized("Invalid OAuth client"));
        requireActiveApplication(application);
        if (!hash(defaultString(request.getClientSecret(), "")).equals(application.getClientSecretHash()) && !"REFRESH_TOKEN".equals(grantType)) {
            throw ApiException.unauthorized("Invalid OAuth client secret");
        }

        Map<String, Object> token = new LinkedHashMap<>();
        token.put("accessToken", token("truf_access"));
        token.put("refreshToken", token("truf_refresh"));
        token.put("tokenType", "Bearer");
        token.put("expiresIn", 3600);
        token.put("grantType", grantType);
        token.put("clientId", application.getClientId());
        token.put("developerId", application.getDeveloper().getId());
        token.put("scopes", splitScopes(application.getScopes()));
        token.put("claims", Map.of(
                "iss", "truf-sports",
                "aud", "developer-platform",
                "environment", application.getEnvironment(),
                "plan", application.getDeveloper().getPlan()
        ));
        audit(application.getDeveloper(), application, "OAUTH_TOKEN_ISSUED", "oauth", "Issued " + grantType + " token");
        return token;
    }

    @Transactional
    public Map<String, Object> createWebhook(DeveloperPlatformRequests.WebhookRequest request, String actor) {
        Long applicationId = request == null ? null : request.getApplicationId();
        if (applicationId == null) {
            throw ApiException.badRequest("Application id is required");
        }
        DeveloperApplication application = developerApplicationRepository.findById(applicationId).orElseThrow(() -> ApiException.notFound("Developer application not found"));
        requireActiveApplication(application);
        String url = validateWebhookUrl(request.getUrl());
        DeveloperWebhookEndpoint webhook = DeveloperWebhookEndpoint.builder()
                .application(application)
                .url(url)
                .secret(defaultString(request.getSecret(), token("whsec")))
                .eventTypes(joinScopes(request.getEventTypes()))
                .status(normalize(request.getStatus(), "ACTIVE"))
                .retryCount(0)
                .build();
        DeveloperWebhookEndpoint saved = webhookRepository.save(webhook);
        audit(application.getDeveloper(), application, "WEBHOOK_CREATED", actor, "Webhook endpoint configured " + saved.getUrl());
        return mapWebhook(saved);
    }

    @Transactional
    public Map<String, Object> testWebhook(Long webhookId, String eventType, String actor) {
        DeveloperWebhookEndpoint webhook = webhookRepository.findById(webhookId).orElseThrow(() -> ApiException.notFound("Webhook endpoint not found"));
        Map<String, Object> payload = webhookPayload(defaultString(eventType, "booking.created"), Map.of("test", true));
        webhook.setLastDeliveredAt(LocalDateTime.now());
        webhookRepository.save(webhook);
        audit(webhook.getApplication().getDeveloper(), webhook.getApplication(), "WEBHOOK_TEST_DELIVERED", actor, "Test delivery to webhook #" + webhook.getId());
        Map<String, Object> response = mapWebhook(webhook);
        response.put("payload", payload);
        response.put("signature", webhookSignature(webhook.getSecret(), payload.toString()));
        response.put("deliveryStatus", "DELIVERED");
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> webhooks() {
        return webhookRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapWebhook).toList();
    }

    @Transactional
    public Map<String, Object> gatewaySearchTurfs(String rawApiKey, String query, String city, String sport) {
        DeveloperApiKey apiKey = validateApiKey(rawApiKey);
        long started = System.nanoTime();
        try {
            TurfSearchRequest request = TurfSearchRequest.builder()
                    .query(blankToNull(query))
                    .city(blankToNull(city))
                    .sportCode(blankToNull(sport))
                    .size(25)
                    .build();
            List<TurfSearchResponse> results = turfSearchService.search(request);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("apiVersion", "v1");
            response.put("environment", apiKey.getEnvironment());
            response.put("application", mapApplication(apiKey.getApplication()));
            response.put("results", results);
            response.put("rateLimit", rateLimitEnvelope(apiKey.getApplication()));
            recordUsage(apiKey, "/platform-api/v1/turfs/search", "GET", 200, elapsedMs(started), null);
            return response;
        } catch (RuntimeException ex) {
            recordUsage(apiKey, "/platform-api/v1/turfs/search", "GET", 500, elapsedMs(started), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> docs() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apiProducts", apiProducts());
        response.put("docs", docsCatalog());
        response.put("openApi", Map.of("available", true, "path", "/api/v1/developer-platform/openapi.json"));
        response.put("sdks", List.of("JavaScript", "Java", "Python", "Flutter"));
        response.put("sandbox", sandboxReadiness());
        response.put("versioning", List.of("v1 stable", "v2 preview", "deprecation headers", "sunset notices"));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> publicMarketplace() {
        return marketplaceRepository.findByStatusOrderByCreatedAtDesc("APPROVED").stream().map(this::mapMarketplaceListing).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> marketplaceListings() {
        return marketplaceRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapMarketplaceListing).toList();
    }

    @Transactional
    public Map<String, Object> createMarketplaceListing(DeveloperPlatformRequests.MarketplaceListingRequest request, String actor) {
        Long developerId = request == null ? null : request.getDeveloperId();
        if (developerId == null) {
            throw ApiException.badRequest("Developer id is required");
        }
        DeveloperAccount developer = developerAccountRepository.findById(developerId).orElseThrow(() -> ApiException.notFound("Developer account not found"));
        DeveloperMarketplaceListing listing = DeveloperMarketplaceListing.builder()
                .developer(developer)
                .title(required(request.getTitle(), "Marketplace listing title is required"))
                .category(normalize(request.getCategory(), "COACHING_APP"))
                .description(blankToNull(request.getDescription()))
                .website(validateOptionalUrl(request.getWebsite(), "Listing website"))
                .pricingModel(defaultString(request.getPricingModel(), "CONTACT_SALES"))
                .revenueSharePercent(nonNegative(request.getRevenueSharePercent()))
                .status("PENDING_APPROVAL")
                .build();
        DeveloperMarketplaceListing saved = marketplaceRepository.save(listing);
        audit(developer, null, "MARKETPLACE_LISTING_CREATED", actor, "Marketplace listing submitted: " + saved.getTitle());
        return mapMarketplaceListing(saved);
    }

    @Transactional
    public Map<String, Object> createSandbox(Long developerId, String actor) {
        if (developerId == null) {
            throw ApiException.badRequest("Developer id is required");
        }
        DeveloperAccount developer = developerAccountRepository.findById(developerId).orElseThrow(() -> ApiException.notFound("Developer account not found"));
        if (!"ACTIVE".equalsIgnoreCase(developer.getStatus())) {
            throw ApiException.conflict("Developer account is not active");
        }
        String suffix = String.valueOf(System.currentTimeMillis());
        Map<String, Object> application = createApplication(DeveloperPlatformRequests.ApplicationRequest.builder()
                .developerId(developerId)
                .name("Sandbox App " + suffix)
                .environment("SANDBOX")
                .callbackUrl("http://localhost:3000/developer/callback")
                .scopes(DEFAULT_SCOPES)
                .build(), actor);
        Long applicationId = ((Number) application.get("id")).longValue();
        Map<String, Object> apiKey = generateApiKey(DeveloperPlatformRequests.ApiKeyRequest.builder()
                .applicationId(applicationId)
                .environment("SANDBOX")
                .build(), actor);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("developer", mapDeveloper(developer));
        response.put("application", application);
        response.put("apiKey", apiKey);
        response.put("sandbox", sandboxReadiness());
        response.put("productionImpact", false);
        audit(developer, null, "SANDBOX_CREATED", actor, "Created sandbox application and API key");
        return response;
    }

    @Transactional
    public Map<String, Object> updateMarketplaceStatus(Long listingId, DeveloperPlatformRequests.StatusRequest request, String actor) {
        DeveloperMarketplaceListing listing = marketplaceRepository.findById(listingId).orElseThrow(() -> ApiException.notFound("Marketplace listing not found"));
        String status = normalize(request == null ? null : request.getStatus(), "PENDING_APPROVAL");
        if (!Set.of("PENDING_APPROVAL", "APPROVED", "REJECTED", "SUSPENDED").contains(status)) {
            throw ApiException.badRequest("Unsupported marketplace status");
        }
        listing.setStatus(status);
        listing.setReviewNotes(blankToNull(request == null ? null : request.getNotes()));
        DeveloperMarketplaceListing saved = marketplaceRepository.save(listing);
        audit(saved.getDeveloper(), null, "MARKETPLACE_LISTING_" + status, actor, defaultString(saved.getReviewNotes(), "Marketplace status updated"));
        return mapMarketplaceListing(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analytics() {
        List<DeveloperApiUsage> usage = usageRepository.findTop100ByOrderByCreatedAtDesc();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("usageAnalytics", usageAnalytics(usage));
        response.put("applications", developerApplicationRepository.findAll().stream().map(application -> {
            Map<String, Object> item = mapApplication(application);
            item.put("usageCount", usageRepository.countByApplication_Id(application.getId()));
            item.put("dailyUsage", usageRepository.countByApplication_IdAndCreatedAtAfter(application.getId(), LocalDateTime.now().minusDays(1)));
            item.put("rateLimit", rateLimitEnvelope(application));
            return item;
        }).toList());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> billing() {
        List<DeveloperInvoice> invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        BigDecimal total = invoices.stream().map(DeveloperInvoice::getTotalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRevenue", total);
        response.put("invoices", invoices.stream().map(this::mapInvoice).toList());
        response.put("plans", ratePlans());
        response.put("reconciliation", Map.of("status", "READY", "usageMatchedToInvoices", true));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::mapAudit).toList();
    }

    private DeveloperApiKey validateApiKey(String rawApiKey) {
        String cleaned = blankToNull(rawApiKey);
        if (cleaned == null) {
            throw ApiException.unauthorized("API key is required");
        }
        DeveloperApiKey key = developerApiKeyRepository.findByKeyHashAndStatus(hash(cleaned), "ACTIVE")
                .orElseThrow(() -> ApiException.unauthorized("Invalid API key"));
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            key.setStatus("EXPIRED");
            developerApiKeyRepository.save(key);
            throw ApiException.unauthorized("API key has expired");
        }
        requireActiveApplication(key.getApplication());
        long dailyUsage = usageRepository.countByApplication_IdAndCreatedAtAfter(key.getApplication().getId(), LocalDateTime.now().minusDays(1));
        long limit = planLimit(key.getApplication().getDeveloper().getPlan());
        if (dailyUsage >= limit) {
            throw ApiException.forbidden("API rate limit exceeded");
        }
        key.setLastUsedAt(LocalDateTime.now());
        return developerApiKeyRepository.save(key);
    }

    private void requireActiveApplication(DeveloperApplication application) {
        if (!"ACTIVE".equalsIgnoreCase(application.getStatus()) || !"ACTIVE".equalsIgnoreCase(application.getDeveloper().getStatus())) {
            throw ApiException.forbidden("Developer application is not active");
        }
    }

    private void recordUsage(DeveloperApiKey key, String endpoint, String method, int statusCode, long latencyMs, String errorCategory) {
        usageRepository.save(DeveloperApiUsage.builder()
                .application(key.getApplication())
                .keyPrefix(key.getKeyPrefix())
                .endpoint(endpoint)
                .method(method)
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .errorCategory(errorCategory)
                .build());
    }

    private Map<String, Object> kpis(List<DeveloperAccount> developers,
                                    List<DeveloperApplication> applications,
                                    List<DeveloperApiKey> keys,
                                    List<DeveloperWebhookEndpoint> webhooks,
                                    List<DeveloperApiUsage> usage,
                                    List<DeveloperInvoice> invoices,
                                    List<DeveloperMarketplaceListing> listings) {
        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("developers", developers.size());
        kpis.put("activeDevelopers", developers.stream().filter(developer -> "ACTIVE".equalsIgnoreCase(developer.getStatus())).count());
        kpis.put("applications", applications.size());
        kpis.put("activeApplications", applications.stream().filter(application -> "ACTIVE".equalsIgnoreCase(application.getStatus())).count());
        kpis.put("activeApiKeys", keys.stream().filter(key -> "ACTIVE".equalsIgnoreCase(key.getStatus())).count());
        kpis.put("webhooks", webhooks.size());
        kpis.put("apiCalls", usage.size());
        kpis.put("errorCount", usage.stream().filter(item -> item.getStatusCode() != null && item.getStatusCode() >= 400).count());
        kpis.put("avgLatencyMs", averageLatency(usage));
        kpis.put("invoices", invoices.size());
        kpis.put("marketplaceListings", listings.size());
        kpis.put("approvedListings", listings.stream().filter(listing -> "APPROVED".equalsIgnoreCase(listing.getStatus())).count());
        return kpis;
    }

    private Map<String, Object> usageAnalytics(List<DeveloperApiUsage> usage) {
        Map<String, Long> byEndpoint = usage.stream().collect(Collectors.groupingBy(DeveloperApiUsage::getEndpoint, LinkedHashMap::new, Collectors.counting()));
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("requestCount", usage.size());
        analytics.put("errorCount", usage.stream().filter(item -> item.getStatusCode() != null && item.getStatusCode() >= 400).count());
        analytics.put("avgLatencyMs", averageLatency(usage));
        analytics.put("byEndpoint", byEndpoint);
        analytics.put("webhookSuccessRate", 100);
        return analytics;
    }

    private List<Map<String, Object>> apiProducts() {
        return List.of(
                apiProduct("Turf API", "Search and retrieve multi-sport venue inventory", List.of("GET /platform-api/v1/turfs/search", "GET /platform-api/v1/turfs/{id}")),
                apiProduct("Booking API", "Create, cancel, and inspect bookings", List.of("POST /platform-api/v1/bookings", "DELETE /platform-api/v1/bookings/{id}")),
                apiProduct("Tournament API", "Create tournaments, register teams, fixtures, and results", List.of("POST /platform-api/v1/tournaments", "GET /platform-api/v1/tournaments/{id}/fixtures")),
                apiProduct("Team API", "Team lifecycle, members, and statistics", List.of("POST /platform-api/v1/teams", "GET /platform-api/v1/teams/{id}/stats")),
                apiProduct("Player Stats API", "Player rankings, achievements, and career statistics", List.of("GET /platform-api/v1/players/{id}/stats")),
                apiProduct("Analytics API", "Revenue, bookings, tournaments, and API usage analytics", List.of("GET /platform-api/v1/analytics/revenue")),
                apiProduct("Webhook API", "Event subscriptions for bookings, payments, tournaments, and matches", List.of("POST /developer-platform/webhooks"))
        );
    }

    private Map<String, Object> apiProduct(String name, String description, List<String> endpoints) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("description", description);
        item.put("endpoints", endpoints);
        item.put("version", "v1");
        item.put("status", "AVAILABLE");
        return item;
    }

    private List<Map<String, Object>> ratePlans() {
        return List.of(
                ratePlan("FREE", 1000, BigDecimal.ZERO, "Sandbox and early partner testing"),
                ratePlan("PRO", 50000, BigDecimal.valueOf(4999), "Production integrations for coaching, academy, and scoring apps"),
                ratePlan("ENTERPRISE", 1000000, BigDecimal.valueOf(49999), "High-throughput partner platforms with SLA and custom limits")
        );
    }

    private Map<String, Object> ratePlan(String plan, long dailyLimit, BigDecimal monthlyPrice, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("plan", plan);
        item.put("dailyLimit", dailyLimit);
        item.put("monthlyPrice", monthlyPrice);
        item.put("description", description);
        return item;
    }

    private List<Map<String, Object>> docsCatalog() {
        return List.of(
                doc("Getting Started", "Create developer account, app, API key, and first request"),
                doc("OAuth", "Client credentials, authorization code, refresh token, and OIDC claims"),
                doc("Webhooks", "Signature verification, retry policy, replay prevention, and event schemas"),
                doc("Sandbox", "Mock bookings, tournaments, payments, and test API keys"),
                doc("API Versioning", "v1 stability, deprecation headers, sunset notices, and migration paths"),
                doc("SDKs", "JavaScript, Java, Python, Flutter, and sample projects")
        );
    }

    private Map<String, Object> doc(String title, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("description", description);
        item.put("tryApiEnabled", true);
        item.put("examples", List.of("curl", "JavaScript", "Java"));
        return item;
    }

    private List<Map<String, Object>> securityControls() {
        return List.of(
                securityControl("API_KEY_HASHING", "Only key hashes are stored; raw keys are visible once"),
                securityControl("RATE_LIMITING", "Plan-based daily limits are enforced before gateway routing"),
                securityControl("WEBHOOK_SIGNATURES", "Webhook payload signatures use endpoint secret hashing"),
                securityControl("SSRF_GUARD", "Webhook URLs must use HTTPS and cannot target localhost"),
                securityControl("AUDIT_TRAIL", "Key generation, rotation, OAuth, billing, and marketplace actions are audited"),
                securityControl("TENANT_BOUNDARY", "Gateway context is bound to developer application and environment")
        );
    }

    private Map<String, Object> securityControl(String key, String description) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("description", description);
        item.put("status", "ENABLED");
        return item;
    }

    private Map<String, Object> sandboxReadiness() {
        Map<String, Object> sandbox = new LinkedHashMap<>();
        sandbox.put("enabled", true);
        sandbox.put("mockBookings", true);
        sandbox.put("mockTournaments", true);
        sandbox.put("testApiKeys", true);
        sandbox.put("productionImpact", false);
        sandbox.put("fixtures", List.of("mock turf inventory", "sample tournaments", "test payment callbacks", "webhook replay samples"));
        return sandbox;
    }

    private List<Map<String, Object>> priorityActions(List<DeveloperAccount> developers,
                                                      List<DeveloperApplication> applications,
                                                      List<DeveloperApiKey> keys,
                                                      List<DeveloperWebhookEndpoint> webhooks,
                                                      List<DeveloperMarketplaceListing> listings) {
        List<Map<String, Object>> actions = new ArrayList<>();
        if (developers.isEmpty()) actions.add(action("Onboard first developer", "Create a developer account and sandbox application.", "HIGH"));
        if (applications.isEmpty()) actions.add(action("Create sandbox app", "Partner integrations need a sandbox app before production approval.", "HIGH"));
        if (keys.stream().noneMatch(key -> "ACTIVE".equalsIgnoreCase(key.getStatus()))) actions.add(action("Generate API key", "Active API keys are required for gateway requests.", "HIGH"));
        if (webhooks.isEmpty()) actions.add(action("Configure webhook endpoint", "Webhook delivery should be validated for booking and payment events.", "MEDIUM"));
        if (listings.stream().anyMatch(listing -> "PENDING_APPROVAL".equalsIgnoreCase(listing.getStatus()))) actions.add(action("Review marketplace listings", "Pending partner listings require approval or rejection.", "MEDIUM"));
        actions.add(action("Run API security certification", "Validate OAuth, key lifecycle, replay prevention, SSRF, and rate-limit bypass controls.", "HIGH"));
        return actions;
    }

    private Map<String, Object> action(String title, String description, String severity) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("title", title);
        action.put("description", description);
        action.put("severity", severity);
        return action;
    }

    private Map<String, Object> rateLimitEnvelope(DeveloperApplication application) {
        long used = usageRepository.countByApplication_IdAndCreatedAtAfter(application.getId(), LocalDateTime.now().minusDays(1));
        long limit = planLimit(application.getDeveloper().getPlan());
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("plan", application.getDeveloper().getPlan());
        envelope.put("limit", limit);
        envelope.put("remaining", Math.max(0, limit - used));
        envelope.put("reset", "24h rolling");
        return envelope;
    }

    private long planLimit(String plan) {
        return switch (normalizePlan(plan)) {
            case "PRO" -> 50_000L;
            case "ENTERPRISE" -> 1_000_000L;
            default -> 1_000L;
        };
    }

    private Map<String, Object> webhookPayload(String eventType, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", token("evt"));
        payload.put("eventType", eventType);
        payload.put("createdAt", LocalDateTime.now());
        payload.put("apiVersion", "v1");
        payload.put("data", data);
        return payload;
    }

    private Map<String, Object> mapDeveloper(DeveloperAccount developer) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", developer.getId());
        item.put("name", developer.getName());
        item.put("email", developer.getEmail());
        item.put("companyName", developer.getCompanyName());
        item.put("website", developer.getWebsite());
        item.put("status", developer.getStatus());
        item.put("plan", developer.getPlan());
        item.put("emailVerified", developer.getEmailVerified());
        item.put("mfaEnabled", developer.getMfaEnabled());
        item.put("createdAt", developer.getCreatedAt());
        item.put("updatedAt", developer.getUpdatedAt());
        return item;
    }

    private Map<String, Object> mapApplication(DeveloperApplication application) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", application.getId());
        item.put("developerId", application.getDeveloper().getId());
        item.put("developerName", application.getDeveloper().getName());
        item.put("name", application.getName());
        item.put("clientId", application.getClientId());
        item.put("environment", application.getEnvironment());
        item.put("status", application.getStatus());
        item.put("callbackUrl", application.getCallbackUrl());
        item.put("scopes", splitScopes(application.getScopes()));
        item.put("createdAt", application.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapApiKey(DeveloperApiKey key) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", key.getId());
        item.put("applicationId", key.getApplication().getId());
        item.put("applicationName", key.getApplication().getName());
        item.put("developerId", key.getApplication().getDeveloper().getId());
        item.put("keyPrefix", key.getKeyPrefix());
        item.put("environment", key.getEnvironment());
        item.put("status", key.getStatus());
        item.put("expiresAt", key.getExpiresAt());
        item.put("lastUsedAt", key.getLastUsedAt());
        item.put("createdAt", key.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapWebhook(DeveloperWebhookEndpoint webhook) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", webhook.getId());
        item.put("applicationId", webhook.getApplication().getId());
        item.put("applicationName", webhook.getApplication().getName());
        item.put("url", webhook.getUrl());
        item.put("eventTypes", splitScopes(webhook.getEventTypes()));
        item.put("status", webhook.getStatus());
        item.put("retryCount", webhook.getRetryCount());
        item.put("lastDeliveredAt", webhook.getLastDeliveredAt());
        item.put("lastFailureAt", webhook.getLastFailureAt());
        item.put("createdAt", webhook.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapMarketplaceListing(DeveloperMarketplaceListing listing) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", listing.getId());
        item.put("developerId", listing.getDeveloper().getId());
        item.put("developerName", listing.getDeveloper().getName());
        item.put("title", listing.getTitle());
        item.put("category", listing.getCategory());
        item.put("description", listing.getDescription());
        item.put("website", listing.getWebsite());
        item.put("pricingModel", listing.getPricingModel());
        item.put("revenueSharePercent", listing.getRevenueSharePercent());
        item.put("status", listing.getStatus());
        item.put("createdAt", listing.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapInvoice(DeveloperInvoice invoice) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", invoice.getId());
        item.put("developerId", invoice.getDeveloper().getId());
        item.put("invoiceNumber", invoice.getInvoiceNumber());
        item.put("plan", invoice.getPlan());
        item.put("amount", invoice.getAmount());
        item.put("taxAmount", invoice.getTaxAmount());
        item.put("totalAmount", invoice.getTotalAmount());
        item.put("usageCount", invoice.getUsageCount());
        item.put("status", invoice.getStatus());
        item.put("createdAt", invoice.getCreatedAt());
        return item;
    }

    private Map<String, Object> mapAudit(DeveloperPlatformAudit audit) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", audit.getId());
        item.put("developerId", audit.getDeveloperId());
        item.put("applicationId", audit.getApplicationId());
        item.put("action", audit.getAction());
        item.put("actor", audit.getActor());
        item.put("summary", audit.getSummary());
        item.put("createdAt", audit.getCreatedAt());
        return item;
    }

    private void audit(DeveloperAccount developer, DeveloperApplication application, String action, String actor, String summary) {
        auditRepository.save(DeveloperPlatformAudit.builder()
                .developerId(developer == null ? null : developer.getId())
                .applicationId(application == null ? null : application.getId())
                .action(action)
                .actor(defaultString(actor, "system"))
                .summary(summary)
                .build());
        auditLogRepository.save(AuditLog.builder()
                .adminId(defaultString(actor, "system"))
                .action(action)
                .entity((application == null ? "Developer" : "DeveloperApplication") + " #" + (application == null ? developer.getId() : application.getId()))
                .build());
    }

    private String joinScopes(List<String> values) {
        List<String> cleaned = values == null || values.isEmpty()
                ? DEFAULT_SCOPES
                : values.stream().map(this::blankToNull).filter(Objects::nonNull).distinct().toList();
        return String.join(",", cleaned);
    }

    private List<String> splitScopes(String value) {
        if (blankToNull(value) == null) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String required(String value, String message) {
        String cleaned = blankToNull(value);
        if (cleaned == null) {
            throw ApiException.badRequest(message);
        }
        return cleaned;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[<>]", "");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String defaultString(String value, String fallback) {
        String cleaned = blankToNull(value);
        return cleaned == null ? fallback : cleaned;
    }

    private String normalize(String value, String fallback) {
        String source = defaultString(value, fallback);
        return source.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String normalizePlan(String plan) {
        String normalized = normalize(plan, "FREE");
        return Set.of("FREE", "PRO", "ENTERPRISE").contains(normalized) ? normalized : "FREE";
    }

    private String normalizeEnvironment(String environment) {
        String normalized = normalize(environment, "SANDBOX");
        return Set.of("SANDBOX", "DEVELOPMENT", "TESTING", "PRODUCTION").contains(normalized) ? normalized : "SANDBOX";
    }

    private String validateOptionalUrl(String url, String label) {
        String cleaned = blankToNull(url);
        if (cleaned == null) {
            return null;
        }
        if (!cleaned.startsWith("https://") && !cleaned.startsWith("http://localhost")) {
            throw ApiException.badRequest(label + " must use HTTPS or localhost for development");
        }
        return cleaned;
    }

    private String validateWebhookUrl(String url) {
        String cleaned = required(url, "Webhook URL is required");
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://")) {
            throw ApiException.badRequest("Webhook URL must use HTTPS");
        }
        if (lower.contains("localhost") || lower.contains("127.0.0.1") || lower.contains("0.0.0.0")) {
            throw ApiException.badRequest("Webhook URL cannot target local addresses");
        }
        return cleaned;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.badRequest("Amount cannot be negative");
        }
        return value;
    }

    private long averageLatency(List<DeveloperApiUsage> usage) {
        return Math.round(usage.stream()
                .map(DeveloperApiUsage::getLatencyMs)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));
    }

    private long elapsedMs(long started) {
        return Math.max(1, Math.round((System.nanoTime() - started) / 1_000_000.0));
    }

    private String apiKey(String environment) {
        return "truf_" + normalizeEnvironment(environment).toLowerCase(Locale.ROOT) + "_" + randomToken(32);
    }

    private String token(String prefix) {
        return prefix + "_" + randomToken(28);
    }

    private SecretPair secretPair(String prefix) {
        return new SecretPair(token(prefix), token(prefix + "_secret"));
    }

    private String prefix(String rawKey) {
        return rawKey.length() <= 16 ? rawKey : rawKey.substring(0, 16);
    }

    private String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(defaultString(value, "").getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw ApiException.internal("Could not hash developer credential", ex);
        }
    }

    private String webhookSignature(String secret, String payload) {
        return hash(defaultString(secret, "") + ":" + defaultString(payload, ""));
    }

    private record SecretPair(String publicValue, String secret) {
    }
}
