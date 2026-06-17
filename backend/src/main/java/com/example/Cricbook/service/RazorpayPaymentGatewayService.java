package com.example.Cricbook.service;

import com.example.Cricbook.exception.ApiException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RazorpayPaymentGatewayService implements PaymentGatewayService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Override
    public GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) throws Exception {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject options = new JSONObject();
            options.put("amount", amount.multiply(new BigDecimal("100")).intValueExact());
            options.put("currency", currency);
            options.put("receipt", receipt);
            Order order = client.orders.create(options);
            return new GatewayOrder(order.get("id"));
        } catch (RazorpayException ex) {
            throw ApiException.serviceUnavailable("Payment gateway is temporarily unavailable. Please try again.");
        } catch (ArithmeticException | IllegalArgumentException ex) {
            throw ApiException.badRequest("Payment request contains invalid data.");
        }
    }
}
