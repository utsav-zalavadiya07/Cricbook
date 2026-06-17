package com.example.Cricbook.service;

import com.example.Cricbook.dto.PaymentOrderResponse;
import com.example.Cricbook.dto.PaymentVerifyRequest;
import com.example.Cricbook.entity.Booking;
import com.example.Cricbook.entity.Invoice;
import com.example.Cricbook.entity.Payment;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.repository.BookingRepository;
import com.example.Cricbook.repository.InvoiceRepository;
import com.example.Cricbook.repository.PaymentRepository;
import com.example.Cricbook.repository.PaymentTransactionRepository;
import com.example.Cricbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WalletService walletService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentGatewayService paymentGatewayService;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PaymentService paymentService;

    private Booking mockBooking;
    private Payment mockPayment;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        mockBooking = new Booking();
        mockBooking.setId(1L);
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setAmount(new BigDecimal("1000.00"));
        mockBooking.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        mockBooking.setUser(mockUser);

        mockPayment = new Payment();
        mockPayment.setId(1L);
        mockPayment.setBooking(mockBooking);
        mockPayment.setRazorpayOrderId("order_123");
        mockPayment.setAmount(new BigDecimal("1000.00"));
        mockPayment.setStatus("CREATED");

        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "secret");
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.getName()).thenReturn("test@test.com");
    }

    @Test
    void testVerifyPayment_AlreadyProcessed() {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setOrderId("order_123");
        request.setPaymentId("pay_123");
        request.setSignature("signature");

        mockPayment.setStatus("SUCCESS");
        mockPayment.setRazorpayPaymentId("pay_123");

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(mockPayment));
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));

        paymentService.verifyPayment(request);

        verify(paymentRepository, never()).save(any());
        verify(bookingService, never()).confirmBooking(anyLong(), any());
    }

    @Test
    void testVerifyPayment_NotFound() {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setOrderId("order_123");
        request.setPaymentId("pay_123");
        request.setSignature("signature");

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.verifyPayment(request);
        });

        assertEquals("Payment was not found.", exception.getMessage());
    }

    // Creating an order requires testing actual Razorpay client integration which is hard to mock effectively without PowerMock due to new RazorpayClient() inside the method. A better practice is to extract RazorpayClient factory, but we can verify the unhappy paths.

    @Test
    void testCreateOrder_BookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createOrder(1L);
        });

        assertEquals("Booking was not found.", exception.getMessage());
    }

    @Test
    void testCreateOrder_InvalidState() {
        mockBooking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createOrder(1L);
        });

        assertEquals("Booking is not in a payable state.", exception.getMessage());
    }

    @Test
    void testCreateOrder_ReusesExistingCreatedOrder() throws Exception {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));
        when(paymentRepository.findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(eq(1L), anyList()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(mockPayment));

        PaymentOrderResponse response = paymentService.createOrder(1L);

        assertEquals("order_123", response.getOrderId());
        verify(paymentGatewayService, never()).createOrder(any(), any(), any());
    }

    @Test
    void testCreateOrder_RejectsExpiredBookingHold() {
        mockBooking.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));

        Exception exception = assertThrows(RuntimeException.class, () -> paymentService.createOrder(1L));

        assertEquals("Booking hold has expired. Please create a new booking.", exception.getMessage());
    }

    @Test
    void testVerifyPayment_InvalidSignatureDoesNotCancelBooking() {
        PaymentVerifyRequest request = new PaymentVerifyRequest();
        request.setOrderId("order_123");
        request.setPaymentId("pay_123");
        request.setSignature("bad_signature");

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(mockPayment));
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));
        when(paymentRepository.findByRazorpayPaymentId("pay_123")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exception exception = assertThrows(RuntimeException.class, () -> paymentService.verifyPayment(request));

        assertEquals("Payment signature verification failed.", exception.getMessage());
        assertEquals("FAILED", mockPayment.getStatus());
        verify(bookingService, never()).confirmBooking(anyLong(), any());
    }

    @Test
    void testPayBookingWithWallet_ConfirmsBookingAndCreatesInvoice() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(mockUser));
        when(paymentRepository.findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(eq(1L), anyList())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            return payment;
        });
        when(invoiceRepository.findByInvoiceNumber("PAY-10")).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.payBookingWithWallet(1L);

        verify(walletService).debitWallet(eq(mockUser), eq(new BigDecimal("1000.00")), eq("BOOKING_PAYMENT"), any());
        verify(bookingService).confirmBooking(eq(1L), any());
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertEquals("/api/v1/billing/invoices/number/PAY-10/download", invoiceCaptor.getValue().getInvoiceUrl());
    }
}
