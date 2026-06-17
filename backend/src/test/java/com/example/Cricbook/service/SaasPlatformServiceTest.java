package com.example.Cricbook.service;

import com.example.Cricbook.dto.SaasPlatformRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaasPlatformServiceTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private WhiteLabelBrandingRepository brandingRepository;
    @Mock
    private TenantCustomDomainRepository customDomainRepository;
    @Mock
    private TenantFeatureToggleRepository featureToggleRepository;
    @Mock
    private TenantSaasSubscriptionRepository subscriptionRepository;
    @Mock
    private TenantBillingInvoiceRepository invoiceRepository;
    @Mock
    private TenantMarketplaceModuleRepository moduleRepository;
    @Mock
    private TenantModuleInstallationRepository installationRepository;
    @Mock
    private TenantAppBuildRepository appBuildRepository;
    @Mock
    private TenantComplianceRequestRepository complianceRepository;
    @Mock
    private TenantResellerRepository resellerRepository;
    @Mock
    private TenantSaasAuditRepository auditRepository;

    @InjectMocks
    private SaasPlatformService saasPlatformService;

    @BeforeEach
    void setUp() {
        lenient().when(auditRepository.save(any(TenantSaasAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTenantBlocksDuplicateDomainAndCreatesTenant() {
        SaasPlatformRequests.TenantRequest request = new SaasPlatformRequests.TenantRequest();
        request.setName("Dubai Cricket Chain");
        request.setDomain("https://Dubai.Cricbook.test/");
        request.setTenantType("enterprise");
        request.setCountryCode("ae");
        request.setRegionCode("ae-dxb");
        request.setDefaultLocale("ar");
        request.setCurrency("aed");

        when(tenantRepository.existsByNameIgnoreCase("Dubai Cricket Chain")).thenReturn(false);
        when(tenantRepository.existsByDomainIgnoreCase("dubai.cricbook.test")).thenReturn(true);

        assertThrows(ApiException.class, () -> saasPlatformService.createTenant(request, "admin@example.com"));
        verify(tenantRepository, never()).save(any(Tenant.class));

        when(tenantRepository.existsByDomainIgnoreCase("dubai.cricbook.test")).thenReturn(false);
        when(customDomainRepository.existsByDomainIgnoreCase("dubai.cricbook.test")).thenReturn(false);
        when(tenantRepository.existsByTenantCode(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(7L);
            return tenant;
        });

        var response = saasPlatformService.createTenant(request, "admin@example.com");

        assertEquals(7L, response.get("id"));
        assertEquals("dubai.cricbook.test", response.get("domain"));
        assertEquals("ENTERPRISE", response.get("tenantType"));
        assertEquals("AE", response.get("countryCode"));
        assertEquals("AED", response.get("currency"));
        verify(auditRepository).save(any(TenantSaasAudit.class));
    }

    @Test
    void domainVerificationRequiresVerifiedBeforeActivation() {
        TenantCustomDomain domain = TenantCustomDomain.builder()
                .id(15L)
                .tenantId(4L)
                .domain("sports.example.com")
                .verificationToken("truf-verify-token")
                .status("PENDING_VERIFICATION")
                .build();

        when(customDomainRepository.findById(15L)).thenReturn(Optional.of(domain));
        when(customDomainRepository.save(any(TenantCustomDomain.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ApiException.class, () -> saasPlatformService.activateDomain(15L, "admin@example.com"));

        var verified = saasPlatformService.verifyDomain(15L, "admin@example.com");
        assertEquals("VERIFIED", verified.get("status"));

        var activated = saasPlatformService.activateDomain(15L, "admin@example.com");
        assertEquals("ACTIVE", activated.get("status"));
        assertNotNull(activated.get("activatedAt"));
    }

    @Test
    void featureToggleUpsertsTenantScopedFeature() {
        Tenant tenant = Tenant.builder()
                .id(2L)
                .tenantCode("TSAAS-2026-123456")
                .name("Tenant")
                .domain("tenant.example.com")
                .build();
        TenantFeatureToggle existing = TenantFeatureToggle.builder()
                .id(10L)
                .tenantId(2L)
                .featureKey("BOOKINGS")
                .enabled(false)
                .build();
        SaasPlatformRequests.FeatureToggleRequest request = new SaasPlatformRequests.FeatureToggleRequest();
        request.setTenantId(2L);
        request.setFeatureKey("bookings");
        request.setEnabled(true);
        request.setRolloutStrategy("tenant override");

        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(featureToggleRepository.findByTenantIdAndFeatureKeyIgnoreCase(2L, "BOOKINGS")).thenReturn(Optional.of(existing));
        when(featureToggleRepository.save(any(TenantFeatureToggle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = saasPlatformService.setFeatureToggle(request, "admin@example.com");

        assertEquals(10L, response.get("id"));
        assertEquals("BOOKINGS", response.get("featureKey"));
        assertEquals(true, response.get("enabled"));
        assertEquals("TENANT_OVERRIDE", response.get("rolloutStrategy"));
    }

    @Test
    void subscriptionAssignsPlanEntitlementsAndRenews() {
        Tenant tenant = Tenant.builder()
                .id(3L)
                .tenantCode("TSAAS-2026-333333")
                .name("Tenant")
                .domain("tenant.example.com")
                .currency("INR")
                .status("TRIAL")
                .lifecycleStage("TRIAL")
                .build();
        SaasPlatformRequests.SubscriptionRequest request = new SaasPlatformRequests.SubscriptionRequest();
        request.setTenantId(3L);
        request.setPlanCode("enterprise");
        request.setBillingCycle("yearly");
        request.setPrice(new BigDecimal("120000.00"));
        request.setStartDate(LocalDate.of(2026, 6, 1));

        AtomicReference<TenantSaasSubscription> savedSubscription = new AtomicReference<>();
        when(tenantRepository.findById(3L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(TenantSaasSubscription.class))).thenAnswer(invocation -> {
            TenantSaasSubscription subscription = invocation.getArgument(0);
            subscription.setId(9L);
            savedSubscription.set(subscription);
            return subscription;
        });

        var created = saasPlatformService.createSubscription(request, "admin@example.com");

        assertEquals(9L, created.get("id"));
        assertEquals("ENTERPRISE", created.get("planCode"));
        assertEquals(LocalDate.of(2027, 6, 1), created.get("endDate"));
        assertEquals("PAID", tenant.getLifecycleStage());
        assertTrue(String.valueOf(created.get("entitlements")).contains("API_PLATFORM"));

        when(subscriptionRepository.findById(9L)).thenReturn(Optional.of(savedSubscription.get()));

        var renewed = saasPlatformService.renewSubscription(9L, "admin@example.com");

        assertEquals(LocalDate.of(2027, 6, 1), renewed.get("startDate"));
        assertEquals(LocalDate.of(2028, 6, 1), renewed.get("endDate"));
    }

    @Test
    void invoicePaymentHandlesPartialAndBlocksDuplicatePaidInvoice() {
        TenantBillingInvoice invoice = TenantBillingInvoice.builder()
                .id(22L)
                .tenantId(5L)
                .invoiceNumber("TSINV-2026-111111")
                .amount(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("180.00"))
                .paidAmount(BigDecimal.ZERO)
                .currency("INR")
                .dueDate(LocalDate.of(2026, 6, 15))
                .status("OPEN")
                .build();
        SaasPlatformRequests.InvoicePaymentRequest partial = new SaasPlatformRequests.InvoicePaymentRequest();
        partial.setAmount(new BigDecimal("500.00"));
        partial.setPaymentReference("BANK-1");
        SaasPlatformRequests.InvoicePaymentRequest remainder = new SaasPlatformRequests.InvoicePaymentRequest();
        remainder.setAmount(new BigDecimal("680.00"));
        remainder.setPaymentReference("BANK-2");

        when(invoiceRepository.findById(22L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(TenantBillingInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var partiallyPaid = saasPlatformService.payInvoice(22L, partial, "finance@example.com");

        assertEquals("PARTIALLY_PAID", partiallyPaid.get("status"));
        assertEquals(new BigDecimal("500.00"), partiallyPaid.get("paidAmount"));

        var paid = saasPlatformService.payInvoice(22L, remainder, "finance@example.com");

        assertEquals("PAID", paid.get("status"));
        assertEquals(new BigDecimal("1180.00"), paid.get("paidAmount"));
        assertNotNull(invoice.getPaidAt());

        assertThrows(ApiException.class, () -> saasPlatformService.payInvoice(22L, remainder, "finance@example.com"));
    }
}
