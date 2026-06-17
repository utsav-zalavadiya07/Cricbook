package com.example.Cricbook.service;

import com.example.Cricbook.dto.AddMoneyRequest;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock private LoyaltyPointRepository loyaltyPointRepository;
    @Mock private ReferralRepository referralRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private LoyaltyService loyaltyService;

    @Test
    void earnPointsIncrementsBalanceAndPushesNotification() {
        User user = User.builder().id(1L).email("u@test.com").build();
        LoyaltyPoint record = LoyaltyPoint.builder().user(user).points(10).history("").build();

        when(loyaltyPointRepository.findByUserId(1L)).thenReturn(Optional.of(record));

        loyaltyService.earnPoints(user, 15, "Booking turf");

        assertEquals(25, record.getPoints());
        verify(loyaltyPointRepository, times(1)).save(record);
        verify(notificationService, times(1)).pushNotification(eq(1L), eq("u@test.com"), anyString(), anyString());
    }

    @Test
    void redeemPointsDeductsBalanceAndCreditsWallet() {
        User user = User.builder().id(1L).email("u@test.com").build();
        LoyaltyPoint record = LoyaltyPoint.builder().user(user).points(100).history("").build();

        when(userRepository.findByEmailOrPhone("u@test.com")).thenReturn(Optional.of(user));
        when(loyaltyPointRepository.findByUserId(1L)).thenReturn(Optional.of(record));

        // 100 points = Rs 10
        loyaltyService.redeemPoints("u@test.com", 100);

        assertEquals(0, record.getPoints());
        verify(loyaltyPointRepository, times(1)).save(record);
        verify(walletService, times(1)).addMoney(eq("u@test.com"), any(AddMoneyRequest.class));
    }
}
