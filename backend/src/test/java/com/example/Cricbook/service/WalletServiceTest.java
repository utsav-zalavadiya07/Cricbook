package com.example.Cricbook.service;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.entity.User;
import com.example.Cricbook.entity.Wallet;
import com.example.Cricbook.repository.UserRepository;
import com.example.Cricbook.repository.WalletRepository;
import com.example.Cricbook.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;

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
}
