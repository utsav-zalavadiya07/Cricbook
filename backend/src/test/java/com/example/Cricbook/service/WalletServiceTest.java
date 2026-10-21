package com.example.Cricbook.service;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.dto.WalletResponse;
import com.example.Cricbook.dto.WalletTopupOrderRequest;
import com.example.Cricbook.dto.WalletTopupOrderResponse;
import com.example.Cricbook.dto.WalletTopupVerifyRequest;
import com.example.Cricbook.entity.Role;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.Wallet;
import com.example.Cricbook.entity.WalletTransaction;
import com.example.Cricbook.enums.RoleName;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.WalletRepository;
import com.example.Cricbook.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private PaymentGatewayService paymentGatewayService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com").build();
        wallet = Wallet.builder().id(10L).user(user).balance(new BigDecimal("500.00")).build();
    }

    @Test
    void addMoneyRejectsZeroAmount() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> walletService.addMoney("test@test.com", new AddMoneyRequest(BigDecimal.ZERO)));

        assertEquals("Amount must be greater than zero.", exception.getMessage());
    }

    @Test
    void addMoneyRejectsExcessiveAmount() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> walletService.addMoney("test@test.com", new AddMoneyRequest(new BigDecimal("100000.01"))));

        assertEquals("Amount cannot exceed Rs 100000.", exception.getMessage());
    }

    @Test
    void addMoneyCreditsWalletAndTransaction() {
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(10L)).thenReturn(java.util.List.of());

        walletService.addMoney("test@test.com", new AddMoneyRequest(new BigDecimal("250.00")));

        assertEquals(new BigDecimal("750.00"), wallet.getBalance());
        verify(walletTransactionRepository).save(any());
    }

    @Test
    void debitWalletRejectsInsufficientBalance() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> {
                    when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
                    walletService.debitWallet(user, new BigDecimal("600.00"), "BOOKING_PAYMENT", "Booking");
                });

        assertEquals("Insufficient wallet balance. Please add money.", exception.getMessage());
    }

    @Test
    void createTopupOrderRejectsInvalidAmounts() {
        assertThrows(RuntimeException.class,
                () -> walletService.createTopupOrder("test@test.com", new WalletTopupOrderRequest(BigDecimal.ZERO)));

        assertThrows(RuntimeException.class,
                () -> walletService.createTopupOrder("test@test.com", new WalletTopupOrderRequest(new BigDecimal("100000.01"))));

        assertThrows(RuntimeException.class,
                () -> walletService.createTopupOrder("test@test.com", new WalletTopupOrderRequest(new BigDecimal("10.555"))));
    }

    @Test
    void createTopupOrderCallsPaymentGateway() throws Exception {
        ReflectionTestUtils.setField(walletService, "razorpayKeyId", "rzp_test_Tf5RCmM8sHNN54");
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(paymentGatewayService.createOrder(eq(new BigDecimal("500.00")), eq("INR"), anyString()))
                .thenReturn(new PaymentGatewayService.GatewayOrder("order_rzp_wallet_123"));

        WalletTopupOrderResponse response = walletService.createTopupOrder(
                "test@test.com",
                new WalletTopupOrderRequest(new BigDecimal("500.00"))
        );

        assertNotNull(response);
        assertEquals("order_rzp_wallet_123", response.getOrderId());
        assertEquals("rzp_test_Tf5RCmM8sHNN54", response.getKeyId());
        assertEquals(new BigDecimal("500.00"), response.getAmount());

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        assertEquals("PENDING", txCaptor.getValue().getStatus());
        assertEquals("ADD_MONEY", txCaptor.getValue().getType());
        assertEquals("order_rzp_wallet_123", txCaptor.getValue().getReference());
    }

    @Test
    void createTopupOrderSupportsOwnerWallet() throws Exception {
        User owner = User.builder()
                .id(2L)
                .email("owner@test.com")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_OWNER).build()))
                .build();
        Wallet ownerWallet = Wallet.builder()
                .id(20L)
                .user(owner)
                .balance(new BigDecimal("1200.00"))
                .build();

        ReflectionTestUtils.setField(walletService, "razorpayKeyId", "rzp_test_owner");
        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(ownerWallet));
        when(paymentGatewayService.createOrder(eq(new BigDecimal("1499.00")), eq("INR"), anyString()))
                .thenReturn(new PaymentGatewayService.GatewayOrder("order_owner_wallet_123"));

        WalletTopupOrderResponse response = walletService.createTopupOrder(
                "owner@test.com",
                new WalletTopupOrderRequest(new BigDecimal("1499.00"))
        );

        assertEquals("order_owner_wallet_123", response.getOrderId());
        assertEquals(new BigDecimal("1499.00"), response.getAmount());
        verify(walletTransactionRepository).save(argThat(transaction ->
                transaction.getWallet().equals(ownerWallet)
                        && transaction.getStatus().equals("PENDING")
                        && transaction.getType().equals("ADD_MONEY")));
    }

    @Test
    void createTopupOrderGatewayErrorThrowsServiceUnavailable() throws Exception {
        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(paymentGatewayService.createOrder(any(), any(), any()))
                .thenThrow(new RuntimeException("Razorpay connection timeout"));

        assertThrows(RuntimeException.class,
                () -> walletService.createTopupOrder("test@test.com", new WalletTopupOrderRequest(new BigDecimal("1000.00"))));
    }


    @Test
    void verifyTopupPaymentRejectsInvalidSignature() {
        ReflectionTestUtils.setField(walletService, "razorpayKeyId", "rzp_live_realKey123");
        ReflectionTestUtils.setField(walletService, "razorpayKeySecret", "test_secret");

        WalletTransaction pendingTx = WalletTransaction.builder()
                .id(101L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .status("PENDING")
                .reference("order_rzp_123")
                .type("ADD_MONEY")
                .build();

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(10L, "order_rzp_123"))
                .thenReturn(Optional.of(pendingTx));

        WalletTopupVerifyRequest request = WalletTopupVerifyRequest.builder()
                .orderId("order_rzp_123")
                .paymentId("pay_rzp_456")
                .signature("invalid_signature")
                .build();

        Exception ex = assertThrows(RuntimeException.class, () -> walletService.verifyTopupPayment("test@test.com", request));
        assertTrue(ex.getMessage().contains("signature"));
        assertEquals("FAILED", pendingTx.getStatus());
        assertEquals(new BigDecimal("500.00"), wallet.getBalance()); // balance unchanged
    }

    @Test
    void verifyTopupPaymentSuccessCreditsWallet() throws Exception {
        ReflectionTestUtils.setField(walletService, "razorpayKeyId", "rzp_live_realKey123");
        ReflectionTestUtils.setField(walletService, "razorpayKeySecret", "test_secret");

        String orderId = "order_rzp_123";
        String paymentId = "pay_rzp_456";
        String validSignature = calculateHmac(orderId + "|" + paymentId, "test_secret");

        WalletTransaction pendingTx = WalletTransaction.builder()
                .id(101L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .status("PENDING")
                .reference(orderId)
                .type("ADD_MONEY")
                .build();

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(10L, orderId))
                .thenReturn(Optional.of(pendingTx));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(10L)).thenReturn(java.util.List.of());

        WalletTopupVerifyRequest request = WalletTopupVerifyRequest.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .signature(validSignature)
                .build();

        WalletResponse response = walletService.verifyTopupPayment("test@test.com", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), wallet.getBalance());
        assertEquals("SUCCESS", pendingTx.getStatus());
        assertEquals(paymentId, pendingTx.getReference());
        verify(notificationService).pushNotification(eq(1L), eq("test@test.com"), anyString(), anyString());
    }

    @Test
    void verifyTopupPaymentIdempotentForAlreadySuccessful() {
        WalletTransaction successfulTx = WalletTransaction.builder()
                .id(101L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .status("SUCCESS")
                .reference("order_rzp_123")
                .type("ADD_MONEY")
                .build();

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(10L, "order_rzp_123"))
                .thenReturn(Optional.of(successfulTx));
        when(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(10L)).thenReturn(java.util.List.of());

        WalletTopupVerifyRequest request = WalletTopupVerifyRequest.builder()
                .orderId("order_rzp_123")
                .paymentId("pay_rzp_456")
                .signature("any_sig")
                .build();

        WalletResponse response = walletService.verifyTopupPayment("test@test.com", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("500.00"), wallet.getBalance()); // balance not duplicated!
        verify(walletRepository, never()).save(any());
    }

    @Test
    void cancelTopupOrderCancelsPendingTransaction() {
        WalletTransaction pendingTx = WalletTransaction.builder()
                .id(101L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .status("PENDING")
                .reference("order_rzp_cancel")
                .type("ADD_MONEY")
                .build();

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(10L, "order_rzp_cancel"))
                .thenReturn(Optional.of(pendingTx));
        when(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(10L)).thenReturn(java.util.List.of());

        WalletResponse response = walletService.cancelTopupOrder("test@test.com", "order_rzp_cancel");

        assertNotNull(response);
        assertEquals("CANCELLED", pendingTx.getStatus());
        assertEquals(new BigDecimal("500.00"), wallet.getBalance()); // balance unchanged
        verify(walletTransactionRepository).save(pendingTx);
    }

    @Test
    void verifyTopupPaymentRejectsCancelledOrder() {
        WalletTransaction cancelledTx = WalletTransaction.builder()
                .id(101L)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .status("CANCELLED")
                .reference("order_rzp_cancelled")
                .type("ADD_MONEY")
                .build();

        when(userRepository.findByEmailOrPhone("test@test.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(10L, "order_rzp_cancelled"))
                .thenReturn(Optional.of(cancelledTx));

        WalletTopupVerifyRequest request = WalletTopupVerifyRequest.builder()
                .orderId("order_rzp_cancelled")
                .paymentId("pay_123")
                .signature("any_sig")
                .build();

        Exception ex = assertThrows(RuntimeException.class, () -> walletService.verifyTopupPayment("test@test.com", request));
        assertTrue(ex.getMessage().contains("cancelled"));
        assertEquals(new BigDecimal("500.00"), wallet.getBalance()); // balance unchanged
    }


    private String calculateHmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
