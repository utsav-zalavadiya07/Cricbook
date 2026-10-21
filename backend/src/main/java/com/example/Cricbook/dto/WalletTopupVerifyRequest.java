package com.example.Cricbook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTopupVerifyRequest {

    @NotBlank(message = "Order ID is required.")
    private String orderId;

    @NotBlank(message = "Payment ID is required.")
    private String paymentId;

    @NotBlank(message = "Payment signature is required.")
    private String signature;
}
