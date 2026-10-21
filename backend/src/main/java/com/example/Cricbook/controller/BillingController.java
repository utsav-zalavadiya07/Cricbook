package com.example.Cricbook.controller;

import com.example.Cricbook.dto.InvoiceResponse;
import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.InvoiceRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.InvoicePdfService;
import com.example.Cricbook.service.SubscriptionRenewalScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final SubscriptionRenewalScheduler renewalScheduler;
    private final InvoicePdfService invoicePdfService;

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponse>> getMyInvoices(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String type) {
        User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        MembershipPlanType planType = parsePlanType(type);
        List<Invoice> invoices = planType == null
                ? invoiceRepository.findDetailedByUserIdOrderByCreatedAtDesc(user.getId())
                : invoiceRepository.findDetailedByUserIdAndPlanTypeOrderByCreatedAtDesc(user.getId(), planType);

        List<InvoiceResponse> list = invoices.stream()
                .map(this::mapInvoice)
                .toList();

        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/invoices/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        Invoice inv = invoiceRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Invoice was not found."));

        if (!inv.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to access this invoice.");
        }

        return pdfResponse(inv);
    }

    @GetMapping(value = "/invoices/number/{invoiceNumber}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoiceByNumber(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String invoiceNumber) {
        User user = userRepository.findByEmailOrPhone(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("User account was not found."));

        Invoice inv = invoiceRepository.findDetailedByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> ApiException.notFound("Invoice was not found."));

        if (!inv.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to access this invoice.");
        }

        return pdfResponse(inv);
    }

    @PostMapping("/renew/test")
    public ResponseEntity<String> triggerRenewalTest() {
        renewalScheduler.triggerRenewalsForTesting();
        return ResponseEntity.ok("Subscription renewal checks processed successfully.");
    }

    private InvoiceResponse mapInvoice(Invoice inv) {
        BigDecimal amount = inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO;
        BigDecimal taxAmount = inv.getTaxAmount() != null ? inv.getTaxAmount() : BigDecimal.ZERO;
        String planName = "Direct Charge";
        String planType = null;
        var plan = inv.getSubscription() != null ? inv.getSubscription().getPlan() : null;
        if (plan != null) {
            if (plan.getName() != null) {
                planName = plan.getName();
            }
            if (plan.getType() != null) {
                planType = plan.getType().name();
            }
        }

        return InvoiceResponse.builder()
                .id(inv.getId())
                .invoiceNumber(inv.getInvoiceNumber())
                .planName(planName)
                .planType(planType)
                .invoiceUrl(inv.getInvoiceUrl())
                .amount(amount)
                .taxAmount(taxAmount)
                .totalAmount(amount.add(taxAmount))
                .status(inv.getStatus() != null ? inv.getStatus() : "UNKNOWN")
                .createdAt(inv.getCreatedAt())
                .build();
    }

    private ResponseEntity<byte[]> pdfResponse(Invoice invoice) {
        byte[] pdf = invoicePdfService.generateInvoice(invoice);
        String safeInvoiceNumber = invoice.getInvoiceNumber() == null
                ? "invoice"
                : invoice.getInvoiceNumber().replaceAll("[^A-Za-z0-9._-]", "-");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("Cricbook-Invoice-" + safeInvoiceNumber + ".pdf")
                .build());
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    private MembershipPlanType parsePlanType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        try {
            return MembershipPlanType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Unsupported billing plan type.");
        }
    }
}
