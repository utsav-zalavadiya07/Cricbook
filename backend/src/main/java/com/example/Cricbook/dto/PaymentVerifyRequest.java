package com.example.Cricbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyRequest {
    private String orderId;
    private String paymentId;
    private String signature;
    private Long bookingId;
    private java.util.List<Long> bookingIds;
}
