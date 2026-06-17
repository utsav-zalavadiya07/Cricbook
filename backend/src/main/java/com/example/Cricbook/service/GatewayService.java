package com.example.Cricbook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class GatewayService {

    public String createOrder(BigDecimal amount, String currency) {
        log.info("Creating gateway order for amount: {} {}", amount, currency);
        // Simulated Razorpay order creation
        return "order_" + UUID.randomUUID().toString().substring(0, 10);
    }

    public boolean verifySignature(String payload, String signature, String secret) {
        log.info("Verifying webhook signature");
        // Simulated Razorpay signature verification
        return signature != null && !signature.equals("invalid");
    }

    public String refund(String paymentId, BigDecimal amount) {
        log.info("Processing gateway refund for paymentId: {} amount: {}", paymentId, amount);
        // Simulated Razorpay refund
        return "rfnd_" + UUID.randomUUID().toString().substring(0, 10);
    }
}
