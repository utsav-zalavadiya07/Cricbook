package com.example.Cricbook.controller;

import com.example.Cricbook.dto.PaymentOrderResponse;
import com.example.Cricbook.dto.PaymentHistoryResponse;
import com.example.Cricbook.dto.PaymentVerifyRequest;
import com.example.Cricbook.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void testCreateOrder() throws Exception {
        PaymentOrderResponse response = PaymentOrderResponse.builder()
                .orderId("order_123")
                .amount(new BigDecimal("1000"))
                .currency("INR")
                .build();

        when(paymentService.createOrder(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/create-order/1")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_123"));
    }

    @Test
    @WithMockUser
    void testPaymentHistory() throws Exception {
        when(paymentService.getPaymentHistory()).thenReturn(List.of(PaymentHistoryResponse.builder()
                .id(1L)
                .bookingId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status("SUCCESS")
                .build()));

        mockMvc.perform(get("/api/v1/payments/history")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    @WithMockUser
    void testWalletPayment() throws Exception {
        when(paymentService.payBookingWithWallet(1L)).thenReturn(PaymentHistoryResponse.builder()
                .id(1L)
                .bookingId(1L)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status("SUCCESS")
                .build());

        mockMvc.perform(post("/api/v1/payments/wallet/1")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
