package com.example.Cricbook.service;

import com.example.Cricbook.config.InvoiceDocumentProperties;
import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.entity.OwnerSettings;
import com.example.Cricbook.entity.Subscription;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.MembershipPlanType;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.OwnerSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float MARGIN = 44;

    private final InvoiceDocumentProperties properties;
    private final OwnerSettingsRepository ownerSettingsRepository;

    public byte[] generateInvoice(Invoice invoice) {
        if (invoice == null) {
            throw ApiException.notFound("Invoice was not found.");
        }

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawHeader(content, invoice);
                drawParties(content, invoice);
                drawLineItems(content, invoice);
                drawSummary(content, invoice);
                drawFooter(content);
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw ApiException.internal("The invoice could not be prepared right now.", exception);
        }
    }

    private void drawHeader(PDPageContentStream content, Invoice invoice) throws IOException {
        fill(content, 15, 31, 53);
        content.addRect(0, 742, PAGE_WIDTH, 100);
        content.fill();

        text(content, BOLD, 21, MARGIN, 795, value(properties.getSupplierName(), "Cricbook"), 255, 255, 255);
        text(content, REGULAR, 9, MARGIN, 778, "Sports booking and membership platform", 191, 219, 254);
        text(content, BOLD, 20, 430, 796, "INVOICE", 255, 255, 255);
        text(content, REGULAR, 9, 430, 778, safe(invoice.getInvoiceNumber()), 191, 219, 254);

        boolean paid = "PAID".equalsIgnoreCase(invoice.getStatus());
        fill(content, paid ? 5 : 217, paid ? 150 : 119, paid ? 105 : 6);
        content.addRect(430, 752, 92, 18);
        content.fill();
        text(content, BOLD, 8, 442, 758, paid ? "PAYMENT RECEIVED" : statusLabel(invoice.getStatus()), 255, 255, 255);
    }

    private void drawParties(PDPageContentStream content, Invoice invoice) throws IOException {
        User customer = invoice.getUser();
        Optional<OwnerSettings> ownerSettings = ownerSettings(invoice);

        sectionLabel(content, "FROM", MARGIN, 708);
        text(content, BOLD, 11, MARGIN, 689, value(properties.getSupplierName(), "Cricbook"), 15, 23, 42);
        float supplierY = wrappedText(content, REGULAR, 9, MARGIN, 675, 220, value(properties.getSupplierAddress(), "India"), 71, 85, 105);
        if (hasText(properties.getSupplierGstin())) {
            text(content, REGULAR, 9, MARGIN, supplierY - 2, "GSTIN: " + properties.getSupplierGstin().trim(), 71, 85, 105);
            supplierY -= 14;
        }
        text(content, REGULAR, 9, MARGIN, supplierY - 2, value(properties.getSupplierEmail(), "support@cricbook.com"), 71, 85, 105);

        sectionLabel(content, "BILLED TO", 315, 708);
        text(content, BOLD, 11, 315, 689, customer != null ? value(customer.getName(), "Cricbook customer") : "Cricbook customer", 15, 23, 42);
        float customerY = 675;
        if (ownerSettings.map(OwnerSettings::getBusinessName).filter(this::hasText).isPresent()) {
            text(content, REGULAR, 9, 315, customerY, ownerSettings.get().getBusinessName().trim(), 71, 85, 105);
            customerY -= 14;
        }
        if (customer != null && hasText(customer.getEmail())) {
            text(content, REGULAR, 9, 315, customerY, customer.getEmail(), 71, 85, 105);
            customerY -= 14;
        }
        if (ownerSettings.map(OwnerSettings::getBusinessAddress).filter(this::hasText).isPresent()) {
            customerY = wrappedText(content, REGULAR, 9, 315, customerY, 225, ownerSettings.get().getBusinessAddress(), 71, 85, 105);
        }
        if (ownerSettings.map(OwnerSettings::getGstNumber).filter(this::hasText).isPresent()) {
            text(content, REGULAR, 9, 315, customerY, "GSTIN: " + ownerSettings.get().getGstNumber().trim(), 71, 85, 105);
        }

        line(content, MARGIN, 620, PAGE_WIDTH - MARGIN, 620, 226, 232, 240);
        detail(content, "Invoice date", formatDate(invoice.getCreatedAt()), MARGIN, 600);
        detail(content, "Invoice number", safe(invoice.getInvoiceNumber()), 215, 600);
        detail(content, "Place of supply", value(properties.getPlaceOfSupply(), "India"), 405, 600);
    }

    private void drawLineItems(PDPageContentStream content, Invoice invoice) throws IOException {
        float tableTop = 548;
        fill(content, 241, 245, 249);
        content.addRect(MARGIN, tableTop, PAGE_WIDTH - (MARGIN * 2), 30);
        content.fill();
        text(content, BOLD, 8, MARGIN + 12, tableTop + 11, "DESCRIPTION", 71, 85, 105);
        text(content, BOLD, 8, 345, tableTop + 11, "QTY", 71, 85, 105);
        text(content, BOLD, 8, 405, tableTop + 11, "RATE", 71, 85, 105);
        text(content, BOLD, 8, 495, tableTop + 11, "AMOUNT", 71, 85, 105);

        text(content, BOLD, 10, MARGIN + 12, tableTop - 24, serviceDescription(invoice), 15, 23, 42);
        text(content, REGULAR, 8, MARGIN + 12, tableTop - 40, servicePeriod(invoice), 100, 116, 139);
        if (hasText(properties.getServiceAccountingCode())) {
            text(content, REGULAR, 8, MARGIN + 12, tableTop - 54, "SAC: " + properties.getServiceAccountingCode().trim(), 100, 116, 139);
        }
        text(content, REGULAR, 10, 350, tableTop - 24, "1", 15, 23, 42);
        rightText(content, REGULAR, 10, 472, tableTop - 24, money(invoice.getAmount()), 15, 23, 42);
        rightText(content, BOLD, 10, PAGE_WIDTH - MARGIN, tableTop - 24, money(invoice.getAmount()), 15, 23, 42);
        line(content, MARGIN, tableTop - 68, PAGE_WIDTH - MARGIN, tableTop - 68, 226, 232, 240);
    }

    private void drawSummary(PDPageContentStream content, Invoice invoice) throws IOException {
        BigDecimal subtotal = amount(invoice.getAmount());
        BigDecimal tax = amount(invoice.getTaxAmount());
        BigDecimal total = subtotal.add(tax);
        float startY = 430;

        text(content, REGULAR, 9, 350, startY, "Taxable value", 71, 85, 105);
        rightText(content, REGULAR, 9, PAGE_WIDTH - MARGIN, startY, money(subtotal), 15, 23, 42);
        text(content, REGULAR, 9, 350, startY - 24, "GST included (" + taxRate(subtotal, tax) + ")", 71, 85, 105);
        rightText(content, REGULAR, 9, PAGE_WIDTH - MARGIN, startY - 24, money(tax), 15, 23, 42);
        line(content, 350, startY - 38, PAGE_WIDTH - MARGIN, startY - 38, 203, 213, 225);
        text(content, BOLD, 11, 350, startY - 61, "Total paid", 15, 23, 42);
        rightText(content, BOLD, 11, PAGE_WIDTH - MARGIN, startY - 61, money(total), 2, 132, 199);

        fill(content, 239, 246, 255);
        content.addRect(MARGIN, 292, PAGE_WIDTH - (MARGIN * 2), 52);
        content.fill();
        text(content, BOLD, 9, MARGIN + 14, 320, "Payment status", 30, 64, 175);
        text(content, REGULAR, 9, MARGIN + 14, 304, paymentSentence(invoice), 71, 85, 105);
    }

    private void drawFooter(PDPageContentStream content) throws IOException {
        line(content, MARGIN, 96, PAGE_WIDTH - MARGIN, 96, 226, 232, 240);
        text(content, BOLD, 9, MARGIN, 73, "Thank you for choosing Cricbook.", 15, 23, 42);
        text(content, REGULAR, 8, MARGIN, 57, "For billing help, contact " + value(properties.getSupplierEmail(), "support@cricbook.com") + ".", 100, 116, 139);
        text(content, REGULAR, 7, MARGIN, 39, "This invoice was generated electronically from your Cricbook payment record.", 148, 163, 184);
        rightText(content, REGULAR, 7, PAGE_WIDTH - MARGIN, 39, "Page 1 of 1", 148, 163, 184);
    }

    private Optional<OwnerSettings> ownerSettings(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        if (invoice.getUser() == null || invoice.getUser().getId() == null || subscription == null || subscription.getPlan() == null
                || subscription.getPlan().getType() != MembershipPlanType.OWNER_FEATURED) {
            return Optional.empty();
        }
        return ownerSettingsRepository.findByOwnerId(invoice.getUser().getId());
    }

    private String serviceDescription(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        if (subscription != null && subscription.getPlan() != null && hasText(subscription.getPlan().getName())) {
            return fit(subscription.getPlan().getName() + " membership plan", 44);
        }
        return "Cricbook booking payment";
    }

    private String servicePeriod(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        if (subscription == null || subscription.getStartDate() == null || subscription.getEndDate() == null) {
            return "Service provided through Cricbook";
        }
        return "Service period: " + subscription.getStartDate().toLocalDate() + " to " + subscription.getEndDate().toLocalDate();
    }

    private String paymentSentence(Invoice invoice) {
        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            return "Paid in full on " + formatDate(invoice.getCreatedAt()) + ". No amount is due.";
        }
        return "Current status: " + statusLabel(invoice.getStatus()) + ".";
    }

    private String taxRate(BigDecimal subtotal, BigDecimal tax) {
        if (subtotal.signum() <= 0 || tax.signum() <= 0) {
            return "0%";
        }
        return tax.multiply(BigDecimal.valueOf(100)).divide(subtotal, 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return "INR " + String.format(Locale.ENGLISH, "%,.2f", amount(value));
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "Not available" : value.format(DATE_FORMAT);
    }

    private String statusLabel(String status) {
        if (!hasText(status)) return "STATUS PENDING";
        return status.trim().replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private void sectionLabel(PDPageContentStream content, String label, float x, float y) throws IOException {
        text(content, BOLD, 8, x, y, label, 2, 132, 199);
    }

    private void detail(PDPageContentStream content, String label, String value, float x, float y) throws IOException {
        text(content, REGULAR, 8, x, y, label, 100, 116, 139);
        text(content, BOLD, 9, x, y - 16, fit(value, 27), 15, 23, 42);
    }

    private float wrappedText(PDPageContentStream content, PDFont font, float size, float x, float y, float maxWidth, String value, int red, int green, int blue) throws IOException {
        String[] words = safe(value).split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        float currentY = y;
        for (String word : words) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (!currentLine.isEmpty() && width(font, size, candidate) > maxWidth) {
                text(content, font, size, x, currentY, currentLine.toString(), red, green, blue);
                currentY -= 13;
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }
        if (!currentLine.isEmpty()) {
            text(content, font, size, x, currentY, currentLine.toString(), red, green, blue);
            currentY -= 13;
        }
        return currentY;
    }

    private void rightText(PDPageContentStream content, PDFont font, float size, float rightX, float y, String value, int red, int green, int blue) throws IOException {
        String safeValue = safe(value);
        text(content, font, size, rightX - width(font, size, safeValue), y, safeValue, red, green, blue);
    }

    private float width(PDFont font, float size, String value) throws IOException {
        return font.getStringWidth(safe(value)) / 1000 * size;
    }

    private void text(PDPageContentStream content, PDFont font, float size, float x, float y, String value, int red, int green, int blue) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
        content.newLineAtOffset(x, y);
        content.showText(safe(value));
        content.endText();
    }

    private void line(PDPageContentStream content, float fromX, float fromY, float toX, float toY, int red, int green, int blue) throws IOException {
        content.setStrokingColor(red / 255f, green / 255f, blue / 255f);
        content.setLineWidth(0.7f);
        content.moveTo(fromX, fromY);
        content.lineTo(toX, toY);
        content.stroke();
    }

    private void fill(PDPageContentStream content, int red, int green, int blue) throws IOException {
        content.setNonStrokingColor(red / 255f, green / 255f, blue / 255f);
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace('–', '-')
                .replace('—', '-')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String fit(String value, int maxLength) {
        String safeValue = safe(value);
        return safeValue.length() <= maxLength ? safeValue : safeValue.substring(0, maxLength - 3) + "...";
    }

    private String value(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
