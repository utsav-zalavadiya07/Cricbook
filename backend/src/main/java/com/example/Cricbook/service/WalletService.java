package com.example.Cricbook.service;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.dto.WalletResponse;
import com.example.Cricbook.dto.WalletTransactionResponse;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.Wallet;
import com.example.Cricbook.entity.WalletTransaction;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.WalletRepository;
import com.example.Cricbook.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final BigDecimal MAX_ADD_MONEY_AMOUNT = new BigDecimal("100000");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public WalletResponse getWallet(String username) {
        Wallet wallet = getOrCreateWallet(findUser(username));
        return mapWallet(wallet);
    }

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
                .description("Money added to wallet")
                .build());

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
}
