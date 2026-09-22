package com.example.Cricbook.service;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.dto.WalletResponse;
import com.example.Cricbook.dto.WalletTopupOrderRequest;
import com.example.Cricbook.dto.WalletTopupOrderResponse;
import com.example.Cricbook.dto.WalletTopupVerifyRequest;
import com.example.Cricbook.dto.WalletTransactionResponse;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.Wallet;
import com.example.Cricbook.entity.WalletTransaction;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.WalletRepository;
import com.example.Cricbook.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private static final BigDecimal MIN_TOPUP_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_ADD_MONEY_AMOUNT = new BigDecimal("100000");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final NotificationService notificationService;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Transactional
    public WalletResponse getWallet(String username) {
        Wallet wallet = getOrCreateWallet(findUser(username));
        return mapWallet(wallet);
    }

    /**
     * Internal wallet credit method (used by LoyaltyService points conversion, Subscriptions, or Admin).
     */
    @Transactional
    public WalletResponse addMoney(String username, AddMoneyRequest request) {
        validateAddMoneyRequest(request);

        Wallet wallet = getOrCreateWallet(findUser(username));
        BigDecimal amount = request.getAmount();
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type("ADD_MONEY")
                .status("SUCCESS")
                .description("Money credited to wallet")
                .build());

        return mapWallet(wallet);
    }

    /**
     * Initiates a top-up payment gateway order (Razorpay) for the authenticated player.
     */
    @Transactional
    public WalletTopupOrderResponse createTopupOrder(String username, WalletTopupOrderRequest request) {
        validateTopupOrderRequest(request);

        User user = findUser(username);
        Wallet wallet = getOrCreateWallet(user);
        BigDecimal amount = request.getAmount();

        String orderId;
        try {
            PaymentGatewayService.GatewayOrder order = paymentGatewayService.createOrder(
                    amount,
                    "INR",
                    "wallet_topup_" + user.getId() + "_" + System.currentTimeMillis()
            );
            orderId = order.orderId();
        } catch (Exception e) {
            log.error("Failed to create Razorpay gateway order for wallet topup", e);
            throw ApiException.serviceUnavailable("Payment gateway is temporarily unavailable. Please try again.");
        }

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type("ADD_MONEY")
                .status("PENDING")
                .reference(orderId)
                .description("Wallet top-up order created")
                .build());

        return WalletTopupOrderResponse.builder()
                .orderId(orderId)
                .amount(amount)
                .currency("INR")
                .keyId(razorpayKeyId)
                .sandbox(false)
                .build();
    }

    /**
     * Verifies payment gateway signature and credits wallet balance upon verified payment.
     */
    @Transactional
    public WalletResponse verifyTopupPayment(String username, WalletTopupVerifyRequest request) {
        validateTopupVerifyRequest(request);

        User user = findUser(username);
        Wallet wallet = getOrCreateWallet(user);

        WalletTransaction tx = walletTransactionRepository
                .findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(wallet.getId(), request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("Top-up transaction order was not found: " + request.getOrderId()));

        // Idempotency check: if transaction is already completed, return existing state safely
        if ("SUCCESS".equalsIgnoreCase(tx.getStatus())) {
            return mapWallet(wallet);
        }

        if ("CANCELLED".equalsIgnoreCase(tx.getStatus())) {
            throw ApiException.conflict("Top-up transaction order was cancelled.");
        }

        if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
            throw ApiException.conflict("Top-up transaction is not in a pending state: " + tx.getStatus());
        }

        // Replay attack prevention: check if this payment ID was already used
        walletTransactionRepository.findFirstByReferenceOrderByCreatedAtDesc(request.getPaymentId())
                .filter(existing -> !existing.getId().equals(tx.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("Payment reference has already been used.");
                });

        // Strictly verify HMAC-SHA256 signature generated by Razorpay
        String expectedSignature = hmacSHA256(
                request.getOrderId() + "|" + request.getPaymentId(),
                razorpayKeySecret
        );

        if (!constantTimeEquals(expectedSignature, request.getSignature())) {
            tx.setStatus("FAILED");
            tx.setDescription("Payment signature verification failed");
            walletTransactionRepository.save(tx);
            log.warn("Wallet top-up signature verification failed for user {} order {}", username, request.getOrderId());
            throw ApiException.badRequest("Payment signature verification failed. Unauthorized payment.");
        }

        // Signature verified successfully -> credit wallet balance
        BigDecimal amount = tx.getAmount();
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        tx.setStatus("SUCCESS");
        tx.setReference(request.getPaymentId());
        tx.setDescription("Wallet recharged via Razorpay (" + request.getPaymentId() + ")");
        walletTransactionRepository.save(tx);

        try {
            notificationService.pushNotification(
                    user.getId(),
                    user.getEmail(),
                    "Wallet Recharged",
                    "Your Cricbook wallet has been recharged with Rs " + amount + ". Transaction ID: " + request.getPaymentId()
            );
        } catch (Exception ex) {
            log.warn("Failed to dispatch wallet top-up notification to {}: {}", user.getEmail(), ex.getMessage());
        }

        return mapWallet(wallet);
    }

    /**
     * Cancels a pending top-up order when the user cancels or closes the payment modal.
     */
    @Transactional
    public WalletResponse cancelTopupOrder(String username, String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw ApiException.badRequest("Order ID is required.");
        }
        User user = findUser(username);
        Wallet wallet = getOrCreateWallet(user);

        walletTransactionRepository
                .findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(wallet.getId(), orderId)
                .ifPresent(tx -> {
                    if ("PENDING".equalsIgnoreCase(tx.getStatus())) {
                        tx.setStatus("CANCELLED");
                        tx.setDescription("Payment cancelled by user");
                        walletTransactionRepository.save(tx);
                        log.info("Wallet top-up order {} cancelled by user {}", orderId, username);
                    }
                });

        return mapWallet(wallet);
    }

    @Transactional
    public void creditRefund(User user, BigDecimal amount, String reference) {
        validatePositiveAmount(amount, "Refund amount must be greater than zero.");
        Wallet wallet = getOrCreateWallet(user);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type("REFUND")
                .status("SUCCESS")
                .reference(reference)
                .description("Refund credited")
                .build());
    }

    @Transactional
    public void debitWallet(User user, BigDecimal amount, String type, String description) {
        validatePositiveAmount(amount, "Payment amount must be greater than zero.");
        Wallet wallet = getOrCreateWallet(user);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ApiException("Insufficient wallet balance. Please add money.");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .type(type)
                .status("SUCCESS")
                .description(description)
                .build());
    }


    private Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .user(user)
                        .balance(BigDecimal.ZERO)
                        .build()));
    }

    private WalletResponse mapWallet(Wallet wallet) {
        List<WalletTransactionResponse> transactions = walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .map(tx -> WalletTransactionResponse.builder()
                        .id(tx.getId())
                        .amount(tx.getAmount())
                        .type(tx.getType())
                        .status(tx.getStatus())
                        .reference(tx.getReference())
                        .description(tx.getDescription())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .toList();

        return WalletResponse.builder()
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .transactions(transactions)
                .build();
    }

    private void validateTopupOrderRequest(WalletTopupOrderRequest request) {
        if (request == null || request.getAmount() == null) {
            throw ApiException.badRequest("Amount is required.");
        }
        if (request.getAmount().compareTo(MIN_TOPUP_AMOUNT) < 0) {
            throw ApiException.badRequest("Amount must be at least Rs 1.00.");
        }
        if (request.getAmount().compareTo(MAX_ADD_MONEY_AMOUNT) > 0) {
            throw ApiException.badRequest("Amount cannot exceed Rs 100000.");
        }
        if (request.getAmount().scale() > 2) {
            throw ApiException.badRequest("Amount can have at most 2 decimal places.");
        }
    }

    private void validateTopupVerifyRequest(WalletTopupVerifyRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Verification request is required.");
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw ApiException.badRequest("Order ID is required.");
        }
        if (request.getPaymentId() == null || request.getPaymentId().trim().isEmpty()) {
            throw ApiException.badRequest("Payment ID is required.");
        }
        if (request.getSignature() == null || request.getSignature().trim().isEmpty()) {
            throw ApiException.badRequest("Payment signature is required.");
        }
    }

    private void validateAddMoneyRequest(AddMoneyRequest request) {
        if (request == null || request.getAmount() == null) {
            throw ApiException.badRequest("Amount is required.");
        }
        validatePositiveAmount(request.getAmount(), "Amount must be greater than zero.");
        if (request.getAmount().compareTo(MAX_ADD_MONEY_AMOUNT) > 0) {
            throw ApiException.badRequest("Amount cannot exceed Rs 100000.");
        }
        if (request.getAmount().scale() > 2) {
            throw ApiException.badRequest("Amount can have at most 2 decimal places.");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest(message);
        }
    }

    private User findUser(String username) {
        return userRepository.findByEmailOrPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String hmacSHA256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
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
        } catch (Exception e) {
            throw ApiException.internal("Payment signature verification algorithm failed.", e);
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
}
