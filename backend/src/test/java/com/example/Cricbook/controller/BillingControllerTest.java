package com.example.Cricbook.controller;

import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.InvoiceRepository;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.service.InvoicePdfService;
import com.example.Cricbook.service.SubscriptionRenewalScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubscriptionRenewalScheduler renewalScheduler;
    @Mock private InvoicePdfService invoicePdfService;

    @InjectMocks
    private BillingController billingController;

    @Test
    void downloadInvoiceReturnsPdfAttachmentForInvoiceOwner() {
        User user = User.builder().id(5L).email("player@example.com").build();
        Invoice invoice = Invoice.builder().id(9L).invoiceNumber("INV-9").user(user).build();
        byte[] pdf = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("player@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(invoiceRepository.findDetailedById(9L)).thenReturn(Optional.of(invoice));
        when(invoicePdfService.generateInvoice(invoice)).thenReturn(pdf);

        ResponseEntity<byte[]> response = billingController.downloadInvoice(principal, 9L);

        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("attachment; filename=\"Cricbook-Invoice-INV-9.pdf\"", response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(pdf, response.getBody());
        verify(invoicePdfService).generateInvoice(invoice);
    }

    @Test
    void downloadInvoiceRejectsAnotherUsersInvoice() {
        User currentUser = User.builder().id(5L).email("player@example.com").build();
        User invoiceOwner = User.builder().id(6L).email("other@example.com").build();
        Invoice invoice = Invoice.builder().id(9L).invoiceNumber("INV-9").user(invoiceOwner).build();
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("player@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(currentUser));
        when(invoiceRepository.findDetailedById(9L)).thenReturn(Optional.of(invoice));

        ApiException exception = assertThrows(ApiException.class, () -> billingController.downloadInvoice(principal, 9L));

        assertEquals("You do not have permission to access this invoice.", exception.getMessage());
    }
}
