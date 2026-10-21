package com.example.Cricbook.service;

import com.example.Cricbook.config.InvoiceDocumentProperties;
import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.entity.OwnerSettings;
import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.repository.OwnerSettingsRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock
    private OwnerSettingsRepository ownerSettingsRepository;

    private InvoicePdfService invoicePdfService;

    @BeforeEach
    void setUp() {
        InvoiceDocumentProperties properties = new InvoiceDocumentProperties();
        properties.setSupplierName("Cricbook Sports Private Limited");
        properties.setSupplierAddress("Surat, Gujarat, India");
        properties.setSupplierGstin("24ABCDE1234F1Z5");
        properties.setSupplierEmail("billing@cricbook.com");
        properties.setServiceAccountingCode("9983");
        properties.setPlaceOfSupply("Gujarat, India");
        invoicePdfService = new InvoicePdfService(properties, ownerSettingsRepository);
    }

    @Test
    void generateInvoiceCreatesReadableOwnerSubscriptionPdf() throws Exception {
        User owner = User.builder()
                .id(7L)
                .name("Utsav Owner")
                .email("owner@example.com")
                .phone("9876543210")
                .build();
        MembershipPlan plan = MembershipPlan.builder()
                .name("BUSINESS")
                .type(MembershipPlanType.OWNER_FEATURED)
                .build();
        Subscription subscription = Subscription.builder()
                .user(owner)
                .plan(plan)
                .startDate(LocalDateTime.of(2026, 10, 12, 10, 0))
                .endDate(LocalDateTime.of(2026, 11, 12, 10, 0))
                .build();
        Invoice invoice = Invoice.builder()
                .id(11L)
                .invoiceNumber("INV-2026-0011")
                .user(owner)
                .subscription(subscription)
                .amount(new BigDecimal("4236.44"))
                .taxAmount(new BigDecimal("762.56"))
                .status("PAID")
                .createdAt(LocalDateTime.of(2026, 10, 12, 10, 5))
                .build();
        OwnerSettings ownerSettings = OwnerSettings.builder()
                .owner(owner)
                .businessName("Central Cricket Arena")
                .businessAddress("Adajan, Surat, Gujarat")
                .gstNumber("24AAAAA0000A1Z5")
                .build();
        when(ownerSettingsRepository.findByOwnerId(7L)).thenReturn(Optional.of(ownerSettings));

        byte[] pdf = invoicePdfService.generateInvoice(invoice);

        assertEquals("%PDF", new String(Arrays.copyOf(pdf, 4), StandardCharsets.US_ASCII));
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("INV-2026-0011"));
            assertTrue(text.contains("Central Cricket Arena"));
            assertTrue(text.contains("BUSINESS membership plan"));
            assertTrue(text.contains("GST included (18%)"));
            assertTrue(text.contains("INR 4,999.00"));
            assertTrue(text.contains("PAYMENT RECEIVED"));
        }
    }
}
