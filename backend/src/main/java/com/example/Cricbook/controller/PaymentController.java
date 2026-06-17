package com.example.Cricbook.controller;

import com.example.Cricbook.dto.PaymentHistoryResponse;
import com.example.Cricbook.dto.PaymentOrderResponse;
import com.example.Cricbook.dto.PaymentVerifyRequest;
import com.example.Cricbook.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/history")
    public ResponseEntity<java.util.List<PaymentHistoryResponse>> history() {
        return ResponseEntity.ok(paymentService.getPaymentHistory());
    }

    @PostMapping("/create-order/{bookingId}")
    public ResponseEntity<PaymentOrderResponse> createOrder(@PathVariable Long bookingId) throws Exception {
        return ResponseEntity.ok(paymentService.createOrder(bookingId));
    }

    @PostMapping("/create-order/batch")
    public ResponseEntity<PaymentOrderResponse> createBatchOrder(@RequestBody PaymentVerifyRequest request) throws Exception {
        return ResponseEntity.ok(paymentService.createBatchOrder(request.getBookingIds()));
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody PaymentVerifyRequest request) {
        paymentService.verifyPayment(request);
        return ResponseEntity.ok("Payment verified and booking confirmed");
    }

    @PostMapping("/wallet/{bookingId}")
    public ResponseEntity<PaymentHistoryResponse> payWithWallet(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.payBookingWithWallet(bookingId));
    }

    @PostMapping("/refund/{bookingId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<String> triggerRefund(@PathVariable Long bookingId) {
        paymentService.triggerRefund(bookingId);
        return ResponseEntity.ok("Refund initiated");
    }
}
