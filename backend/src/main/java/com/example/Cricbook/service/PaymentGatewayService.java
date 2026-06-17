package com.example.Cricbook.service;

import java.math.BigDecimal;

public interface PaymentGatewayService {
    GatewayOrder createOrder(BigDecimal amount, String currency, String receipt) throws Exception;

    record GatewayOrder(String orderId) {
    }
}
