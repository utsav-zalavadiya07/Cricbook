package com.example.Cricbook.service;

import com.example.Cricbook.dto.ConfirmBookingRequest;
import com.example.Cricbook.dto.PaymentHistoryResponse;
import com.example.Cricbook.dto.PaymentOrderResponse;
import com.example.Cricbook.dto.PaymentVerifyRequest;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import com.example.Cricbook.utils.NotificationTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceRepository invoiceRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Transactional
    public PaymentOrderResponse createOrder(Long bookingId) throws Exception {
        if (bookingId == null) {
            throw ApiException.badRequest("Booking is required.");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));

        User currentUser = getCurrentUser();
        validateBookingPaymentAccess(booking, currentUser);
        validatePayableBooking(booking);

        Payment existingPayment = paymentRepository
                .findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(bookingId, List.of("CREATED", "PROCESSING"))
                .orElse(null);
        if (existingPayment != null && existingPayment.getRazorpayOrderId() != null) {
            return paymentOrderResponse(existingPayment.getRazorpayOrderId(), existingPayment.getAmount());
        }

        PaymentGatewayService.GatewayOrder order = paymentGatewayService.createOrder(
                booking.getAmount(),
                "INR",
                "booking_" + bookingId
        );

        Payment payment = Payment.builder()
                .booking(booking)
                .batchBookingIds(String.valueOf(booking.getId()))
                .razorpayOrderId(order.orderId())
                .amount(booking.getAmount())
                .status("CREATED")
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        saveTransaction(savedPayment, "CREATED", "Gateway order created");

        return paymentOrderResponse(order.orderId(), booking.getAmount());
    }

    @Transactional
    public PaymentOrderResponse createBatchOrder(List<Long> bookingIds) throws Exception {
        if (bookingIds == null || bookingIds.isEmpty()) {
            throw ApiException.badRequest("Select at least one booking.");
        }

        List<Long> distinctBookingIds = bookingIds.stream().distinct().collect(Collectors.toList());
        List<Booking> bookings = distinctBookingIds.stream()
                .map(id -> bookingRepository.findById(id)
                        .orElseThrow(() -> ApiException.notFound("Booking was not found: " + id)))
                .collect(Collectors.toList());

        User currentUser = getCurrentUser();
        bookings.forEach(booking -> {
            validateBookingPaymentAccess(booking, currentUser);
            validatePayableBooking(booking);
        });

        BigDecimal totalAmount = bookings.stream()
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PaymentGatewayService.GatewayOrder order = paymentGatewayService.createOrder(
                totalAmount,
                "INR",
                "bookings_" + bookings.get(0).getId()
        );

        Payment payment = Payment.builder()
                .booking(bookings.get(0))
                .batchBookingIds(distinctBookingIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .razorpayOrderId(order.orderId())
                .amount(totalAmount)
                .status("CREATED")
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        saveTransaction(savedPayment, "CREATED", "Gateway batch order created");

        return paymentOrderResponse(order.orderId(), totalAmount);
    }

    @Transactional
    public void verifyPayment(PaymentVerifyRequest request) {
        validateVerifyRequest(request);
        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment was not found."));

        validatePaymentAccess(payment);

        if ("SUCCESS".equals(payment.getStatus())) {
            if (request.getPaymentId().equals(payment.getRazorpayPaymentId())) {
                return;
            }
            throw ApiException.conflict("Payment has already been processed.");
        }

        paymentRepository.findByRazorpayPaymentId(request.getPaymentId())
                .filter(existing -> !existing.getId().equals(payment.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Payment reference has already been used.");
                });

        if (!"CREATED".equalsIgnoreCase(payment.getStatus()) && !"PROCESSING".equalsIgnoreCase(payment.getStatus())) {
            throw ApiException.conflict("Payment is not in a verifiable state.");
        }

        String generatedSignature = HmacSHA256(
                request.getOrderId() + "|" + request.getPaymentId(),
                razorpayKeySecret
        );

        User user = payment.getBooking().getUser();

        if (!constantTimeEquals(generatedSignature, request.getSignature())) {
            payment.setStatus("FAILED");
            Payment failedPayment = paymentRepository.save(payment);
            saveTransaction(failedPayment, "FAILED", "Payment signature verification failed");
            
            notificationService.pushNotification(
                user.getId(),
                user.getEmail(),
                "Payment Failed",
                NotificationTemplates.paymentFailed()
            );
            
            throw ApiException.badRequest("Payment signature verification failed.");
        }

        payment.setStatus("PROCESSING");
        paymentRepository.save(payment);
        saveTransaction(payment, "PROCESSING", "Payment signature verified");

        List<Long> bookingIds = resolvePaymentBookingIds(payment);
        for (Long bookingId : bookingIds) {
            bookingService.confirmBooking(bookingId, new ConfirmBookingRequest(request.getPaymentId(), "SUCCESS"));
        }

        payment.setRazorpayPaymentId(request.getPaymentId());
        payment.setStatus("SUCCESS");
        Payment successfulPayment = paymentRepository.save(payment);
        saveTransaction(successfulPayment, "SUCCESS", "Payment captured and booking confirmed");
        generateInvoiceIfMissing(successfulPayment);

        notificationService.pushNotification(
            user.getId(),
            user.getEmail(),
            "Payment Successful",
            NotificationTemplates.paymentSuccess(payment.getBooking().getId())
        );
    }

    @Transactional
    public PaymentHistoryResponse payBookingWithWallet(Long bookingId) {
        if (bookingId == null) {
            throw ApiException.badRequest("Booking is required.");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        User currentUser = getCurrentUser();
        validateBookingPaymentAccess(booking, currentUser);
        validatePayableBooking(booking);

        String reference = "wallet_booking_" + bookingId;
        Payment payment = Payment.builder()
                .booking(booking)
                .batchBookingIds(String.valueOf(booking.getId()))
                .razorpayOrderId(reference)
                .razorpayPaymentId(reference)
                .amount(booking.getAmount())
                .status("PROCESSING")
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        saveTransaction(savedPayment, "PROCESSING", "Wallet payment started");

        walletService.debitWallet(currentUser, booking.getAmount(), "BOOKING_PAYMENT", "Wallet payment for booking #" + bookingId);
        bookingService.confirmBooking(bookingId, new ConfirmBookingRequest(reference, "SUCCESS"));

        savedPayment.setStatus("SUCCESS");
        savedPayment = paymentRepository.save(savedPayment);
        saveTransaction(savedPayment, "SUCCESS", "Wallet payment completed");
        generateInvoiceIfMissing(savedPayment);
        return mapPaymentHistory(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryResponse> getPaymentHistory() {
        User currentUser = getCurrentUser();
        return paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapPaymentHistory)
                .toList();
    }

    @Transactional
    public void triggerRefund(Long bookingId) {
        if (bookingId == null) {
            throw ApiException.badRequest("Booking is required.");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking was not found."));
        validateRefundAccess(booking);

        Payment payment = paymentRepository.findAll().stream()
                .filter(item -> item.getBooking().getId().equals(bookingId))
                .filter(item -> "SUCCESS".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Successful payment was not found for the booking."));

        payment.setStatus("REFUNDED");
        Payment refundedPayment = paymentRepository.save(payment);
        saveTransaction(refundedPayment, "REFUNDED", "Refund credited to wallet");

        if (walletService != null) {
            walletService.creditRefund(booking.getUser(), payment.getAmount(), "booking_" + bookingId);
        }

        notificationService.pushNotification(
                booking.getUser().getId(),
                booking.getUser().getEmail(),
                "Refund Initiated",
                "Your refund for booking #" + bookingId + " has been credited to your Cricbook wallet."
        );
    }

    private PaymentOrderResponse paymentOrderResponse(String orderId, BigDecimal amount) {
        return PaymentOrderResponse.builder()
                .orderId(orderId)
                .amount(amount.multiply(new BigDecimal("100")))
                .currency("INR")
                .build();
    }

    private void validateVerifyRequest(PaymentVerifyRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Payment verification details are required.");
        }
        if (isBlank(request.getOrderId())) {
            throw ApiException.badRequest("Payment order id is required.");
        }
        if (isBlank(request.getPaymentId())) {
            throw ApiException.badRequest("Payment id is required.");
        }
        if (isBlank(request.getSignature())) {
            throw ApiException.badRequest("Payment signature is required.");
        }
    }

    private void validatePayableBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw ApiException.conflict("Booking is not in a payable state.");
        }
        if (booking.getExpiryTime() != null && booking.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw ApiException.conflict("Booking hold has expired. Please create a new booking.");
        }
        if (booking.getAmount() == null || booking.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Booking amount must be greater than zero.");
        }
        paymentRepository.findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(booking.getId(), List.of("SUCCESS"))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Booking has already been paid.");
                });
    }

    private void validatePaymentAccess(Payment payment) {
        validateBookingPaymentAccess(payment.getBooking(), getCurrentUser());
    }

    private void validateRefundAccess(Booking booking) {
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == com.example.Cricbook.enums.RoleName.ROLE_ADMIN);
        boolean ownsTurf = booking.getTurf() != null
                && booking.getTurf().getOwner() != null
                && booking.getTurf().getOwner().getId().equals(currentUser.getId());
        if (!isAdmin && !ownsTurf) {
            throw ApiException.forbidden("You do not have permission to refund this booking.");
        }
    }

    private void saveTransaction(Payment payment, String status, String response) {
        paymentTransactionRepository.save(PaymentTransaction.builder()
                .payment(payment)
                .status(status)
                .response(response)
                .build());
    }

    private void generateInvoiceIfMissing(Payment payment) {
        String invoiceNumber = "PAY-" + payment.getId();
        if (invoiceRepository.findByInvoiceNumber(invoiceNumber).isPresent()) {
            return;
        }
        invoiceRepository.save(Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .user(payment.getBooking().getUser())
                .amount(payment.getAmount().setScale(2, RoundingMode.HALF_UP))
                .taxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .status("PAID")
                .invoiceUrl("/api/v1/billing/invoices/number/" + invoiceNumber + "/download")
                .build());
    }

    private PaymentHistoryResponse mapPaymentHistory(Payment payment) {
        List<Long> bookingIds = resolvePaymentBookingIds(payment);
        return PaymentHistoryResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .bookingIds(bookingIds)
                .orderId(payment.getRazorpayOrderId())
                .paymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency("INR")
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private String HmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw ApiException.internal("Payment signature algorithm is not available.", e);
        } catch (java.security.InvalidKeyException e) {
            throw ApiException.internal("Payment signature key is invalid.", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private List<Long> resolvePaymentBookingIds(Payment payment) {
        if (payment.getBatchBookingIds() == null || payment.getBatchBookingIds().isBlank()) {
            return List.of(payment.getBooking().getId());
        }
        List<Long> ids = Arrays.stream(payment.getBatchBookingIds().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        return ids.isEmpty() ? new ArrayList<>(List.of(payment.getBooking().getId())) : ids;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> ApiException.notFound("User account was not found."));
    }

    private void validateBookingPaymentAccess(Booking booking, User currentUser) {
        boolean ownsBooking = booking.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == com.example.Cricbook.enums.RoleName.ROLE_ADMIN);
        if (!ownsBooking && !isAdmin) {
            throw ApiException.forbidden("You do not have permission to pay for this booking.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
