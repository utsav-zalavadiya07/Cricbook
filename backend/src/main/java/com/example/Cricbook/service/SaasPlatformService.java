package com.example.Cricbook.service;

import com.example.Cricbook.dto.SaasPlatformRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SaasPlatformService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> TENANT_STATUSES = Set.of("ACTIVE", "SUSPENDED", "ARCHIVED", "TRIAL", "EXPIRED");
    private static final Set<String> LIFECYCLE_STAGES = Set.of("ONBOARDING", "TRIAL", "PAID", "GRACE_PERIOD", "EXPIRED", "SUSPENDED", "ARCHIVED");
    private static final Set<String> PLAN_CODES = Set.of("STARTER", "PROFESSIONAL", "ENTERPRISE", "GOVERNMENT");
    private static final Set<String> BILLING_CYCLES = Set.of("MONTHLY", "QUARTERLY", "YEARLY");
    private static final Set<String> FEATURE_KEYS = Set.of("BOOKINGS", "COMMERCE", "ACADEMY", "TOURNAMENTS", "AI", "CRM", "MARKETPLACE", "API_PLATFORM", "FAN_ECONOMY");
    private static final Set<String> COMPLIANCE_REGULATIONS = Set.of("GDPR", "CCPA", "DPDP", "SOC2", "ISO27001");
    private static final Set<String> COMPLIANCE_TYPES = Set.of("DATA_EXPORT", "DATA_DELETION", "CONSENT_REVIEW", "RETENTION_HOLD", "DATA_RESIDENCY");

    private final TenantRepository tenantRepository;
    private final WhiteLabelBrandingRepository brandingRepository;
    private final TenantCustomDomainRepository customDomainRepository;
    private final TenantFeatureToggleRepository featureToggleRepository;
    private final TenantSaasSubscriptionRepository subscriptionRepository;
    private final TenantBillingInvoiceRepository invoiceRepository;
    private final TenantMarketplaceModuleRepository moduleRepository;
    private final TenantModuleInstallationRepository installationRepository;
    private final TenantAppBuildRepository appBuildRepository;
    private final TenantComplianceRequestRepository complianceRepository;
    private final TenantResellerRepository resellerRepository;
    private final TenantSaasAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> publicTenantContext() {
        return publicTenantContext(null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> publicTenantContext(Long requestedTenantId) {
        Long tenantId = requestedTenantId == null ? TenantContext.getCurrentTenant() : requestedTenantId;
        Tenant tenant = tenant(tenantId);
        Map<String, Object> response = mapTenant(tenant);
        response.put("branding", mapBranding(brandingForTenant(tenantId)));
        response.put("features", featureToggleRepository.findByTenantIdOrderByFeatureKeyAsc(tenantId).stream().map(this::mapFeatureToggle).toList());
        response.put("domains", customDomainRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::mapCustomDomain).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminDashboard() {
        List<Tenant> tenants = tenantRepository.findAllByOrderByIdDesc();
        List<TenantCustomDomain> domains = customDomainRepository.findAllByOrderByCreatedAtDesc();
        List<TenantFeatureToggle> toggles = featureToggleRepository.findAllByOrderByUpdatedAtDesc();
        List<TenantSaasSubscription> subscriptions = subscriptionRepository.findAllByOrderByCreatedAtDesc();
        List<TenantBillingInvoice> invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        List<TenantMarketplaceModule> modules = moduleRepository.findAllByOrderByCreatedAtDesc();
        List<TenantModuleInstallation> installations = installationRepository.findAllByOrderByInstalledAtDesc();
        List<TenantAppBuild> appBuilds = appBuildRepository.findAllByOrderByCreatedAtDesc();
        List<TenantComplianceRequest> compliance = complianceRepository.findAllByOrderByCreatedAtDesc();
        List<TenantReseller> resellers = resellerRepository.findAllByOrderByCreatedAtDesc();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis(tenants, domains, subscriptions, invoices, modules, installations, appBuilds, compliance, resellers));
        response.put("tenants", tenants.stream().limit(25).map(this::mapTenant).toList());
        response.put("domains", domains.stream().limit(25).map(this::mapCustomDomain).toList());
        response.put("featureToggles", toggles.stream().limit(40).map(this::mapFeatureToggle).toList());
        response.put("subscriptions", subscriptions.stream().limit(25).map(this::mapSubscription).toList());
        response.put("invoices", invoices.stream().limit(25).map(this::mapInvoice).toList());
        response.put("marketplaceModules", modules.stream().limit(25).map(this::mapModule).toList());
        response.put("installations", installations.stream().limit(25).map(this::mapInstallation).toList());
        response.put("appBuilds", appBuilds.stream().limit(25).map(this::mapAppBuild).toList());
        response.put("complianceRequests", compliance.stream().limit(25).map(this::mapCompliance).toList());
        response.put("resellers", resellers.stream().limit(25).map(this::mapReseller).toList());
        response.put("globalControls", globalControls());
        response.put("regionReadiness", regionReadiness(tenants));
        response.put("audits", audits());
        response.put("generatedAt", LocalDateTime.now());
        return response;
    }

    @Transactional
    public Map<String, Object> createTenant(SaasPlatformRequests.TenantRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Tenant request is required");
        }
        String name = required(request.getName(), "Tenant name is required");
        String domain = normalizeDomain(required(request.getDomain(), "Tenant domain is required"));
        if (tenantRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Tenant name already exists");
        }
        if (tenantRepository.existsByDomainIgnoreCase(domain) || customDomainRepository.existsByDomainIgnoreCase(domain)) {
            throw ApiException.conflict("Tenant domain already exists");
        }
        String status = normalize(request.getStatus(), "ACTIVE");
        if (!TENANT_STATUSES.contains(status)) {
            throw ApiException.badRequest("Unsupported tenant status");
        }
        String lifecycleStage = normalize(request.getLifecycleStage(), "TRIAL");
        if (!LIFECYCLE_STAGES.contains(lifecycleStage)) {
            throw ApiException.badRequest("Unsupported tenant lifecycle stage");
        }
        Tenant tenant = Tenant.builder()
                .tenantCode(generateTenantCode())
                .name(name)
                .domain(domain)
                .theme("DEFAULT")
                .tenantType(normalize(request.getTenantType(), "STANDARD"))
                .countryCode(defaultString(request.getCountryCode(), "IN").toUpperCase(Locale.ROOT))
                .regionCode(defaultString(request.getRegionCode(), "IN-GJ").toUpperCase(Locale.ROOT))
                .defaultLocale(defaultString(request.getDefaultLocale(), "en").toLowerCase(Locale.ROOT))
                .currency(defaultString(request.getCurrency(), "INR").toUpperCase(Locale.ROOT))
                .timezone(defaultString(request.getTimezone(), "Asia/Kolkata"))
                .status(status)
                .lifecycleStage(lifecycleStage)
                .whiteLabelEnabled(false)
                .build();
        Tenant saved = tenantRepository.save(tenant);
        audit(saved.getId(), "TENANT", saved.getId(), "TENANT_CREATED", actor, saved.getDomain());
        return mapTenant(saved);
    }

    @Transactional
    public Map<String, Object> updateTenantStatus(Long tenantId, SaasPlatformRequests.TenantStatusRequest request, String actor) {
        Tenant tenant = tenant(tenantId);
        if (request == null) {
            throw ApiException.badRequest("Tenant status request is required");
        }
        if (request.getStatus() != null) {
            String status = normalize(request.getStatus(), tenant.getStatus());
            if (!TENANT_STATUSES.contains(status)) {
                throw ApiException.badRequest("Unsupported tenant status");
            }
            tenant.setStatus(status);
        }
        if (request.getLifecycleStage() != null) {
            String stage = normalize(request.getLifecycleStage(), tenant.getLifecycleStage());
            if (!LIFECYCLE_STAGES.contains(stage)) {
                throw ApiException.badRequest("Unsupported tenant lifecycle stage");
            }
            tenant.setLifecycleStage(stage);
        }
        Tenant saved = tenantRepository.save(tenant);
        audit(saved.getId(), "TENANT", saved.getId(), "TENANT_STATUS_UPDATED", actor, defaultString(request.getReason(), saved.getStatus()));
        return mapTenant(saved);
    }

    @Transactional
    public Map<String, Object> saveBranding(SaasPlatformRequests.BrandingRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for branding");
        }
        Tenant tenant = tenant(request.getTenantId());
        WhiteLabelBranding branding = brandingRepository.findByTenantId(tenant.getId()).orElse(WhiteLabelBranding.builder().tenantId(tenant.getId()).build());
        branding.setAppName(defaultString(request.getAppName(), tenant.getName()));
        branding.setLogoUrl(defaultString(request.getLogoUrl(), "/assets/brand-default.png"));
        branding.setFaviconUrl(blankToNull(request.getFaviconUrl()));
        branding.setPrimaryColor(defaultString(request.getPrimaryColor(), "222.2 47.4% 11.2%"));
        branding.setSecondaryColor(defaultString(request.getSecondaryColor(), "142.1 76.2% 36.3%"));
        branding.setFontFamily(defaultString(request.getFontFamily(), "Inter"));
        branding.setPublished(Boolean.TRUE.equals(request.getPublished()));
        WhiteLabelBranding saved = brandingRepository.save(branding);
        tenant.setWhiteLabelEnabled(true);
        tenantRepository.save(tenant);
        audit(tenant.getId(), "WHITE_LABEL_BRANDING", saved.getId(), "BRANDING_UPDATED", actor, saved.getAppName());
        return mapBranding(saved);
    }

    @Transactional
    public Map<String, Object> addDomain(SaasPlatformRequests.CustomDomainRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for domain");
        }
        Tenant tenant = tenant(request.getTenantId());
        String domain = normalizeDomain(required(request.getDomain(), "Custom domain is required"));
        if (tenantRepository.existsByDomainIgnoreCase(domain) || customDomainRepository.existsByDomainIgnoreCase(domain)) {
            throw ApiException.conflict("Domain already exists");
        }
        TenantCustomDomain customDomain = TenantCustomDomain.builder()
                .tenantId(tenant.getId())
                .domain(domain)
                .verificationToken("truf-verify-" + token(16))
                .routingMode(normalize(request.getRoutingMode(), "CNAME"))
                .build();
        TenantCustomDomain saved = customDomainRepository.save(customDomain);
        audit(tenant.getId(), "CUSTOM_DOMAIN", saved.getId(), "DOMAIN_ADDED", actor, saved.getDomain());
        return mapCustomDomain(saved);
    }

    @Transactional
    public Map<String, Object> verifyDomain(Long domainId, String actor) {
        TenantCustomDomain domain = customDomain(domainId);
        if (!"PENDING_VERIFICATION".equalsIgnoreCase(domain.getStatus())) {
            throw ApiException.badRequest("Only pending domains can be verified");
        }
        domain.setStatus("VERIFIED");
        domain.setVerifiedAt(LocalDateTime.now());
        TenantCustomDomain saved = customDomainRepository.save(domain);
        audit(domain.getTenantId(), "CUSTOM_DOMAIN", saved.getId(), "DOMAIN_VERIFIED", actor, saved.getDomain());
        return mapCustomDomain(saved);
    }

    @Transactional
    public Map<String, Object> activateDomain(Long domainId, String actor) {
        TenantCustomDomain domain = customDomain(domainId);
        if (!"VERIFIED".equalsIgnoreCase(domain.getStatus())) {
            throw ApiException.badRequest("Only verified domains can be activated");
        }
        domain.setStatus("ACTIVE");
        domain.setActivatedAt(LocalDateTime.now());
        TenantCustomDomain saved = customDomainRepository.save(domain);
        audit(domain.getTenantId(), "CUSTOM_DOMAIN", saved.getId(), "DOMAIN_ACTIVATED", actor, saved.getDomain());
        return mapCustomDomain(saved);
    }

    @Transactional
    public Map<String, Object> setFeatureToggle(SaasPlatformRequests.FeatureToggleRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for feature toggle");
        }
        Tenant tenant = tenant(request.getTenantId());
        String featureKey = normalize(request.getFeatureKey(), "");
        if (!FEATURE_KEYS.contains(featureKey)) {
            throw ApiException.badRequest("Unsupported tenant feature key");
        }
        TenantFeatureToggle toggle = featureToggleRepository.findByTenantIdAndFeatureKeyIgnoreCase(tenant.getId(), featureKey)
                .orElse(TenantFeatureToggle.builder().tenantId(tenant.getId()).featureKey(featureKey).build());
        toggle.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        toggle.setRolloutStrategy(normalize(request.getRolloutStrategy(), "TENANT_OVERRIDE"));
        toggle.setNotes(blankToNull(request.getNotes()));
        TenantFeatureToggle saved = featureToggleRepository.save(toggle);
        audit(tenant.getId(), "FEATURE_TOGGLE", saved.getId(), "FEATURE_TOGGLE_UPDATED", actor, featureKey + "=" + saved.getEnabled());
        return mapFeatureToggle(saved);
    }

    @Transactional
    public Map<String, Object> createSubscription(SaasPlatformRequests.SubscriptionRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for subscription");
        }
        Tenant tenant = tenant(request.getTenantId());
        String planCode = normalize(request.getPlanCode(), "STARTER");
        if (!PLAN_CODES.contains(planCode)) {
            throw ApiException.badRequest("Unsupported SaaS plan");
        }
        String billingCycle = normalize(request.getBillingCycle(), "MONTHLY");
        if (!BILLING_CYCLES.contains(billingCycle)) {
            throw ApiException.badRequest("Unsupported billing cycle");
        }
        LocalDate start = request.getStartDate() == null ? LocalDate.now() : request.getStartDate();
        LocalDate end = request.getEndDate() == null ? defaultEndDate(start, billingCycle) : request.getEndDate();
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("Subscription end date must be after start date");
        }
        TenantSaasSubscription subscription = TenantSaasSubscription.builder()
                .tenantId(tenant.getId())
                .planCode(planCode)
                .billingCycle(billingCycle)
                .price(money(request.getPrice(), "Subscription price"))
                .currency(defaultString(request.getCurrency(), tenant.getCurrency()).toUpperCase(Locale.ROOT))
                .autoRenew(!Boolean.FALSE.equals(request.getAutoRenew()))
                .startDate(start)
                .endDate(end)
                .status("ACTIVE")
                .entitlements(entitlementsForPlan(planCode))
                .build();
        TenantSaasSubscription saved = subscriptionRepository.save(subscription);
        tenant.setLifecycleStage("PAID");
        tenant.setStatus("ACTIVE");
        tenantRepository.save(tenant);
        audit(tenant.getId(), "SAAS_SUBSCRIPTION", saved.getId(), "SUBSCRIPTION_CREATED", actor, planCode);
        return mapSubscription(saved);
    }

    @Transactional
    public Map<String, Object> renewSubscription(Long subscriptionId, String actor) {
        TenantSaasSubscription subscription = subscription(subscriptionId);
        LocalDate previousEnd = subscription.getEndDate();
        subscription.setStartDate(previousEnd);
        subscription.setEndDate(defaultEndDate(previousEnd, subscription.getBillingCycle()));
        subscription.setStatus("ACTIVE");
        TenantSaasSubscription saved = subscriptionRepository.save(subscription);
        audit(saved.getTenantId(), "SAAS_SUBSCRIPTION", saved.getId(), "SUBSCRIPTION_RENEWED", actor, saved.getEndDate().toString());
        return mapSubscription(saved);
    }

    @Transactional
    public Map<String, Object> generateInvoice(SaasPlatformRequests.InvoiceRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for invoice");
        }
        Tenant tenant = tenant(request.getTenantId());
        BigDecimal amount = money(request.getAmount(), "Invoice amount");
        BigDecimal tax = defaultMoney(request.getTaxAmount());
        TenantBillingInvoice invoice = TenantBillingInvoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .tenantId(tenant.getId())
                .subscriptionId(request.getSubscriptionId())
                .amount(amount)
                .taxAmount(tax)
                .currency(defaultString(request.getCurrency(), tenant.getCurrency()).toUpperCase(Locale.ROOT))
                .dueDate(request.getDueDate() == null ? LocalDate.now().plusDays(15) : request.getDueDate())
                .status("OPEN")
                .build();
        TenantBillingInvoice saved = invoiceRepository.save(invoice);
        audit(tenant.getId(), "TENANT_INVOICE", saved.getId(), "INVOICE_GENERATED", actor, saved.getInvoiceNumber());
        return mapInvoice(saved);
    }

    @Transactional
    public Map<String, Object> payInvoice(Long invoiceId, SaasPlatformRequests.InvoicePaymentRequest request, String actor) {
        TenantBillingInvoice invoice = invoice(invoiceId);
        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw ApiException.conflict("Invoice is already paid");
        }
        BigDecimal payment = money(request == null ? null : request.getAmount(), "Payment amount");
        BigDecimal outstanding = invoice.getAmount().add(invoice.getTaxAmount()).subtract(invoice.getPaidAmount());
        if (payment.compareTo(outstanding) > 0) {
            throw ApiException.badRequest("Payment amount exceeds outstanding invoice balance");
        }
        invoice.setPaidAmount(invoice.getPaidAmount().add(payment).setScale(2, RoundingMode.HALF_UP));
        String paymentReference = request == null ? null : request.getPaymentReference();
        invoice.setPaymentReference(defaultString(paymentReference, "MANUAL-" + token(8)));
        if (invoice.getPaidAmount().compareTo(invoice.getAmount().add(invoice.getTaxAmount())) >= 0) {
            invoice.setStatus("PAID");
            invoice.setPaidAt(LocalDateTime.now());
        } else {
            invoice.setStatus("PARTIALLY_PAID");
        }
        TenantBillingInvoice saved = invoiceRepository.save(invoice);
        audit(saved.getTenantId(), "TENANT_INVOICE", saved.getId(), "INVOICE_PAYMENT_RECORDED", actor, saved.getStatus());
        return mapInvoice(saved);
    }

    @Transactional
    public Map<String, Object> publishMarketplaceModule(SaasPlatformRequests.MarketplaceModuleRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Marketplace module request is required");
        }
        String moduleCode = normalize(request.getModuleCode(), "");
        if (moduleCode.isBlank()) {
            throw ApiException.badRequest("Module code is required");
        }
        if (moduleRepository.existsByModuleCodeIgnoreCase(moduleCode)) {
            throw ApiException.conflict("Marketplace module already exists");
        }
        TenantMarketplaceModule module = TenantMarketplaceModule.builder()
                .moduleCode(moduleCode)
                .name(required(request.getName(), "Module name is required"))
                .category(normalize(request.getCategory(), "CORE"))
                .pricingModel(normalize(request.getPricingModel(), "FREE"))
                .price(defaultMoney(request.getPrice()))
                .currency(defaultString(request.getCurrency(), "INR").toUpperCase(Locale.ROOT))
                .certificationStatus(normalize(request.getCertificationStatus(), "CERTIFIED"))
                .build();
        TenantMarketplaceModule saved = moduleRepository.save(module);
        audit(null, "MARKETPLACE_MODULE", saved.getId(), "MODULE_PUBLISHED", actor, saved.getModuleCode());
        return mapModule(saved);
    }

    @Transactional
    public Map<String, Object> installModule(SaasPlatformRequests.ModuleInstallationRequest request, String actor) {
        if (request == null || request.getTenantId() == null || request.getModuleId() == null) {
            throw ApiException.badRequest("Tenant id and module id are required");
        }
        Tenant tenant = tenant(request.getTenantId());
        TenantMarketplaceModule module = module(request.getModuleId());
        if (!"PUBLISHED".equalsIgnoreCase(module.getStatus())) {
            throw ApiException.badRequest("Only published modules can be installed");
        }
        installationRepository.findByTenantIdAndModuleId(tenant.getId(), module.getId()).ifPresent(existing -> {
            throw ApiException.conflict("Module already installed for this tenant");
        });
        TenantModuleInstallation installation = TenantModuleInstallation.builder()
                .tenantId(tenant.getId())
                .moduleId(module.getId())
                .licenseType(defaultString(request.getLicenseType(), module.getPricingModel()).toUpperCase(Locale.ROOT))
                .status("INSTALLED")
                .build();
        TenantModuleInstallation saved = installationRepository.save(installation);
        audit(tenant.getId(), "MODULE_INSTALLATION", saved.getId(), "MODULE_INSTALLED", actor, module.getModuleCode());
        return mapInstallation(saved);
    }

    @Transactional
    public Map<String, Object> requestAppBuild(SaasPlatformRequests.AppBuildRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for app build");
        }
        Tenant tenant = tenant(request.getTenantId());
        TenantAppBuild build = TenantAppBuild.builder()
                .tenantId(tenant.getId())
                .appType(normalize(request.getAppType(), "WHITE_LABEL"))
                .platform(normalize(request.getPlatform(), "WEB"))
                .version(defaultString(request.getVersion(), "1.0.0"))
                .artifactUrl("pending://" + tenant.getTenantCode() + "/" + token(8))
                .status("QUEUED")
                .build();
        TenantAppBuild saved = appBuildRepository.save(build);
        audit(tenant.getId(), "APP_BUILD", saved.getId(), "APP_BUILD_REQUESTED", actor, saved.getPlatform());
        return mapAppBuild(saved);
    }

    @Transactional
    public Map<String, Object> createComplianceRequest(SaasPlatformRequests.ComplianceRequest request, String actor) {
        if (request == null || request.getTenantId() == null) {
            throw ApiException.badRequest("Tenant id is required for compliance request");
        }
        Tenant tenant = tenant(request.getTenantId());
        String regulation = normalize(request.getRegulation(), "DPDP");
        if (!COMPLIANCE_REGULATIONS.contains(regulation)) {
            throw ApiException.badRequest("Unsupported compliance regulation");
        }
        String requestType = normalize(request.getRequestType(), "DATA_EXPORT");
        if (!COMPLIANCE_TYPES.contains(requestType)) {
            throw ApiException.badRequest("Unsupported compliance request type");
        }
        TenantComplianceRequest compliance = TenantComplianceRequest.builder()
                .tenantId(tenant.getId())
                .regulation(regulation)
                .requestType(requestType)
                .subjectEmail(blankToNull(request.getSubjectEmail()))
                .notes(blankToNull(request.getNotes()))
                .build();
        TenantComplianceRequest saved = complianceRepository.save(compliance);
        audit(tenant.getId(), "COMPLIANCE_REQUEST", saved.getId(), "COMPLIANCE_REQUEST_CREATED", actor, regulation + "/" + requestType);
        return mapCompliance(saved);
    }

    @Transactional
    public Map<String, Object> registerReseller(SaasPlatformRequests.ResellerRequest request, String actor) {
        if (request == null) {
            throw ApiException.badRequest("Reseller request is required");
        }
        String email = required(request.getEmail(), "Reseller email is required").toLowerCase(Locale.ROOT);
        if (resellerRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Reseller already exists");
        }
        TenantReseller reseller = TenantReseller.builder()
                .name(required(request.getName(), "Reseller name is required"))
                .email(email)
                .regionCode(defaultString(request.getRegionCode(), "IN").toUpperCase(Locale.ROOT))
                .commissionPercent(percent(request.getCommissionPercent(), "Reseller commission"))
                .parentResellerId(request.getParentResellerId())
                .build();
        TenantReseller saved = resellerRepository.save(reseller);
        audit(null, "TENANT_RESELLER", saved.getId(), "RESELLER_REGISTERED", actor, saved.getEmail());
        return mapReseller(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tenants() {
        return tenantRepository.findAllByOrderByIdDesc().stream().map(this::mapTenant).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> marketplaceModules() {
        return moduleRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream().map(this::mapModule).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> regions() {
        return regionReadiness(tenantRepository.findAllByOrderByIdDesc());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> audits() {
        return auditRepository.findAllByOrderByCreatedAtDesc().stream().limit(75).map(this::mapAudit).toList();
    }

    private Map<String, Object> kpis(List<Tenant> tenants,
                                     List<TenantCustomDomain> domains,
                                     List<TenantSaasSubscription> subscriptions,
                                     List<TenantBillingInvoice> invoices,
                                     List<TenantMarketplaceModule> modules,
                                     List<TenantModuleInstallation> installations,
                                     List<TenantAppBuild> appBuilds,
                                     List<TenantComplianceRequest> compliance,
                                     List<TenantReseller> resellers) {
        BigDecimal mrr = subscriptions.stream()
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .map(item -> "YEARLY".equalsIgnoreCase(item.getBillingCycle()) ? item.getPrice().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP) : item.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstanding = invoices.stream()
                .filter(item -> !"PAID".equalsIgnoreCase(item.getStatus()))
                .map(item -> item.getAmount().add(item.getTaxAmount()).subtract(item.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tenants", tenants.size());
        map.put("activeTenants", tenants.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count());
        map.put("whiteLabelTenants", tenants.stream().filter(item -> Boolean.TRUE.equals(item.getWhiteLabelEnabled())).count());
        map.put("countries", tenants.stream().map(Tenant::getCountryCode).filter(item -> item != null && !item.isBlank()).distinct().count());
        map.put("customDomains", domains.size());
        map.put("activeDomains", domains.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus())).count());
        map.put("subscriptions", subscriptions.size());
        map.put("mrr", mrr);
        map.put("outstandingInvoices", outstanding);
        map.put("marketplaceModules", modules.size());
        map.put("installations", installations.size());
        map.put("appBuilds", appBuilds.size());
        map.put("openComplianceRequests", compliance.stream().filter(item -> "OPEN".equalsIgnoreCase(item.getStatus())).count());
        map.put("resellers", resellers.size());
        return map;
    }

    private List<Map<String, Object>> globalControls() {
        return List.of(
                Map.of("name", "Tenant context", "status", "ACTIVE", "detail", "X-Tenant-ID and domain resolution drive tenant context."),
                Map.of("name", "Data isolation", "status", "ACTIVE", "detail", "Tenant-scoped SaaS records enforce tenant ids at service boundaries."),
                Map.of("name", "Feature toggles", "status", "ACTIVE", "detail", "Tenant features follow a vendor-neutral flag evaluation model."),
                Map.of("name", "Subscription entitlements", "status", "ACTIVE", "detail", "Plan codes map to feature entitlement bundles."),
                Map.of("name", "Compliance workflow", "status", "ACTIVE", "detail", "GDPR, CCPA, DPDP, SOC2 and ISO requests are tracked by tenant.")
        );
    }

    private List<Map<String, Object>> regionReadiness(List<Tenant> tenants) {
        return List.of(
                region("IN", "India", tenants),
                region("AE", "UAE", tenants),
                region("US", "USA", tenants),
                region("AU", "Australia", tenants),
                region("GB", "United Kingdom", tenants)
        );
    }

    private Map<String, Object> region(String code, String label, List<Tenant> tenants) {
        long count = tenants.stream().filter(item -> code.equalsIgnoreCase(item.getCountryCode())).count();
        return Map.of(
                "countryCode", code,
                "label", label,
                "tenants", count,
                "status", count > 0 ? "ACTIVE" : "READY",
                "dataResidency", code.equals("IN") ? "DPDP_READY" : "REGION_REVIEW"
        );
    }

    private Map<String, Object> mapTenant(Tenant tenant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tenant.getId());
        map.put("tenantCode", tenant.getTenantCode());
        map.put("name", tenant.getName());
        map.put("domain", tenant.getDomain());
        map.put("tenantType", tenant.getTenantType());
        map.put("countryCode", tenant.getCountryCode());
        map.put("regionCode", tenant.getRegionCode());
        map.put("defaultLocale", tenant.getDefaultLocale());
        map.put("currency", tenant.getCurrency());
        map.put("timezone", tenant.getTimezone());
        map.put("status", tenant.getStatus());
        map.put("lifecycleStage", tenant.getLifecycleStage());
        map.put("whiteLabelEnabled", tenant.getWhiteLabelEnabled());
        map.put("createdAt", tenant.getCreatedAt());
        return map;
    }

    private Map<String, Object> mapBranding(WhiteLabelBranding branding) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", branding.getId());
        map.put("tenantId", branding.getTenantId());
        map.put("appName", branding.getAppName());
        map.put("logoUrl", branding.getLogoUrl());
        map.put("faviconUrl", branding.getFaviconUrl());
        map.put("primaryColor", branding.getPrimaryColor());
        map.put("secondaryColor", branding.getSecondaryColor());
        map.put("fontFamily", branding.getFontFamily());
        map.put("published", branding.getPublished());
        return map;
    }

    private Map<String, Object> mapCustomDomain(TenantCustomDomain domain) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", domain.getId());
        map.put("tenantId", domain.getTenantId());
        map.put("domain", domain.getDomain());
        map.put("verificationToken", domain.getVerificationToken());
        map.put("routingMode", domain.getRoutingMode());
        map.put("status", domain.getStatus());
        map.put("verifiedAt", domain.getVerifiedAt());
        map.put("activatedAt", domain.getActivatedAt());
        return map;
    }

    private Map<String, Object> mapFeatureToggle(TenantFeatureToggle toggle) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", toggle.getId());
        map.put("tenantId", toggle.getTenantId());
        map.put("featureKey", toggle.getFeatureKey());
        map.put("enabled", toggle.getEnabled());
        map.put("rolloutStrategy", toggle.getRolloutStrategy());
        map.put("notes", toggle.getNotes());
        return map;
    }

    private Map<String, Object> mapSubscription(TenantSaasSubscription subscription) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", subscription.getId());
        map.put("tenantId", subscription.getTenantId());
        map.put("planCode", subscription.getPlanCode());
        map.put("billingCycle", subscription.getBillingCycle());
        map.put("price", subscription.getPrice());
        map.put("currency", subscription.getCurrency());
        map.put("autoRenew", subscription.getAutoRenew());
        map.put("startDate", subscription.getStartDate());
        map.put("endDate", subscription.getEndDate());
        map.put("status", subscription.getStatus());
        map.put("entitlements", subscription.getEntitlements());
        return map;
    }

    private Map<String, Object> mapInvoice(TenantBillingInvoice invoice) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", invoice.getId());
        map.put("invoiceNumber", invoice.getInvoiceNumber());
        map.put("tenantId", invoice.getTenantId());
        map.put("subscriptionId", invoice.getSubscriptionId());
        map.put("amount", invoice.getAmount());
        map.put("taxAmount", invoice.getTaxAmount());
        map.put("paidAmount", invoice.getPaidAmount());
        map.put("currency", invoice.getCurrency());
        map.put("dueDate", invoice.getDueDate());
        map.put("status", invoice.getStatus());
        map.put("paymentReference", invoice.getPaymentReference());
        return map;
    }

    private Map<String, Object> mapModule(TenantMarketplaceModule module) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", module.getId());
        map.put("moduleCode", module.getModuleCode());
        map.put("name", module.getName());
        map.put("category", module.getCategory());
        map.put("pricingModel", module.getPricingModel());
        map.put("price", module.getPrice());
        map.put("currency", module.getCurrency());
        map.put("certificationStatus", module.getCertificationStatus());
        map.put("status", module.getStatus());
        return map;
    }

    private Map<String, Object> mapInstallation(TenantModuleInstallation installation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", installation.getId());
        map.put("tenantId", installation.getTenantId());
        map.put("moduleId", installation.getModuleId());
        map.put("licenseType", installation.getLicenseType());
        map.put("status", installation.getStatus());
        map.put("installedAt", installation.getInstalledAt());
        return map;
    }

    private Map<String, Object> mapAppBuild(TenantAppBuild build) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", build.getId());
        map.put("tenantId", build.getTenantId());
        map.put("appType", build.getAppType());
        map.put("platform", build.getPlatform());
        map.put("version", build.getVersion());
        map.put("artifactUrl", build.getArtifactUrl());
        map.put("status", build.getStatus());
        return map;
    }

    private Map<String, Object> mapCompliance(TenantComplianceRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", request.getId());
        map.put("tenantId", request.getTenantId());
        map.put("regulation", request.getRegulation());
        map.put("requestType", request.getRequestType());
        map.put("subjectEmail", request.getSubjectEmail());
        map.put("notes", request.getNotes());
        map.put("status", request.getStatus());
        return map;
    }

    private Map<String, Object> mapReseller(TenantReseller reseller) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", reseller.getId());
        map.put("name", reseller.getName());
        map.put("email", reseller.getEmail());
        map.put("regionCode", reseller.getRegionCode());
        map.put("commissionPercent", reseller.getCommissionPercent());
        map.put("parentResellerId", reseller.getParentResellerId());
        map.put("status", reseller.getStatus());
        return map;
    }

    private Map<String, Object> mapAudit(TenantSaasAudit audit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", audit.getId());
        map.put("tenantId", audit.getTenantId());
        map.put("entityType", audit.getEntityType());
        map.put("entityId", audit.getEntityId());
        map.put("action", audit.getAction());
        map.put("actor", audit.getActor());
        map.put("details", audit.getDetails());
        map.put("createdAt", audit.getCreatedAt());
        return map;
    }

    private WhiteLabelBranding brandingForTenant(Long tenantId) {
        Tenant tenant = tenant(tenantId);
        return brandingRepository.findByTenantId(tenantId).orElseGet(() -> WhiteLabelBranding.builder()
                .tenantId(tenantId)
                .appName(tenant.getName())
                .logoUrl("/assets/brand-default.png")
                .primaryColor("222.2 47.4% 11.2%")
                .secondaryColor("142.1 76.2% 36.3%")
                .fontFamily("Inter")
                .published(false)
                .build());
    }

    private Tenant tenant(Long tenantId) {
        if (tenantId == null) {
            throw ApiException.badRequest("Tenant id is required");
        }
        return tenantRepository.findById(tenantId).orElseThrow(() -> ApiException.notFound("Tenant not found"));
    }

    private TenantCustomDomain customDomain(Long domainId) {
        if (domainId == null) {
            throw ApiException.badRequest("Domain id is required");
        }
        return customDomainRepository.findById(domainId).orElseThrow(() -> ApiException.notFound("Custom domain not found"));
    }

    private TenantSaasSubscription subscription(Long subscriptionId) {
        if (subscriptionId == null) {
            throw ApiException.badRequest("Subscription id is required");
        }
        return subscriptionRepository.findById(subscriptionId).orElseThrow(() -> ApiException.notFound("Subscription not found"));
    }

    private TenantBillingInvoice invoice(Long invoiceId) {
        if (invoiceId == null) {
            throw ApiException.badRequest("Invoice id is required");
        }
        return invoiceRepository.findById(invoiceId).orElseThrow(() -> ApiException.notFound("Invoice not found"));
    }

    private TenantMarketplaceModule module(Long moduleId) {
        if (moduleId == null) {
            throw ApiException.badRequest("Module id is required");
        }
        return moduleRepository.findById(moduleId).orElseThrow(() -> ApiException.notFound("Marketplace module not found"));
    }

    private void audit(Long tenantId, String entityType, Long entityId, String action, String actor, String details) {
        auditRepository.save(TenantSaasAudit.builder()
                .tenantId(tenantId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(defaultString(actor, "system"))
                .details(blankToNull(details))
                .build());
    }

    private String entitlementsForPlan(String planCode) {
        return switch (planCode) {
            case "GOVERNMENT" -> "SPORTS_OS,BOOKINGS,TOURNAMENTS,REPORTING,COMPLIANCE";
            case "ENTERPRISE" -> "BOOKINGS,COMMERCE,ACADEMY,TOURNAMENTS,AI,CRM,MARKETPLACE,API_PLATFORM,FAN_ECONOMY";
            case "PROFESSIONAL" -> "BOOKINGS,COMMERCE,TOURNAMENTS,CRM,MARKETPLACE";
            default -> "BOOKINGS,TOURNAMENTS";
        };
    }

    private LocalDate defaultEndDate(LocalDate start, String billingCycle) {
        return switch (normalize(billingCycle, "MONTHLY")) {
            case "YEARLY" -> start.plusYears(1);
            case "QUARTERLY" -> start.plusMonths(3);
            default -> start.plusMonths(1);
        };
    }

    private String generateTenantCode() {
        return unique("TSAAS-", tenantRepository::existsByTenantCode);
    }

    private String generateInvoiceNumber() {
        return unique("TSINV-", invoiceRepository::existsByInvoiceNumber);
    }

    private String unique(String prefix, java.util.function.Predicate<String> exists) {
        String value;
        do {
            value = prefix + LocalDate.now().getYear() + "-" + (100000 + RANDOM.nextInt(900000));
        } while (exists.test(value));
        return value;
    }

    private String token(int size) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase(Locale.ROOT).replace("https://", "").replace("http://", "").replaceAll("/+$", "");
    }

    private String normalize(String value, String fallback) {
        return defaultString(value, fallback).trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String required(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw ApiException.badRequest(message);
        }
        return normalized;
    }

    private String defaultString(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal money(BigDecimal value, String label) {
        BigDecimal amount = defaultMoney(value);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest(label + " must be greater than zero");
        }
        return amount;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal value, String label) {
        BigDecimal number = defaultMoney(value);
        if (number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest(label + " must be between 0 and 100");
        }
        return number;
    }
}
