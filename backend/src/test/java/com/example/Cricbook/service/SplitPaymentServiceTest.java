package com.example.Cricbook.service;

import com.example.Cricbook.dto.SplitPaymentDtos;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.CollectionStatus;
import com.example.Cricbook.enums.PaymentSplitStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SplitPaymentServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentSplitRepository splitRepository;
    @Mock private TeamCollectionRepository collectionRepository;
    @Mock private PaymentRuleRepository ruleRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private RecurringCollectionRepository recurringRepository;
    @Mock private PaymentSettlementRepository settlementRepository;
    @Mock private WalletService walletService;
    @Mock private NotificationService notificationService;
    @InjectMocks private SplitPaymentService service;

    @Test
    void createSplitDividesBookingAmountAndCreatesCollection() {
        User captain = user(1L, "Captain");
        User player = user(2L, "Player");
        Booking booking = booking(captain);
        SplitPaymentDtos.CreateSplitRequest request = new SplitPaymentDtos.CreateSplitRequest();
        request.setBookingId(10L);
        request.setShares(List.of(share(1L, null), share(2L, null)));

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(captain));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(splitRepository.findByBookingIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(captain));
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(splitRepository.save(any(PaymentSplit.class))).thenAnswer(invocation -> {
            PaymentSplit split = invocation.getArgument(0);
            split.setId(split.getUser().getId() + 100);
            return split;
        });
        when(collectionRepository.save(any(TeamCollection.class))).thenAnswer(invocation -> {
            TeamCollection collection = invocation.getArgument(0);
            collection.setId(50L);
            return collection;
        });

        SplitPaymentDtos.SplitPaymentResponse response = service.createSplit("captain@test.com", request);

        assertEquals(2, response.getSplits().size());
        assertEquals(0, BigDecimal.valueOf(1200).compareTo(response.getSplits().get(0).getAmount()));
        assertEquals(CollectionStatus.OPEN, response.getCollection().getStatus());
        verify(notificationService).pushNotification(eq(1L), eq("captain@test.com"), eq("Payment Share Created"), anyString());
        verify(notificationService).pushNotification(eq(2L), eq("player@test.com"), eq("Payment Share Created"), anyString());
    }

    @Test
    void customSplitMustMatchBookingTotal() {
        User captain = user(1L, "Captain");
        Booking booking = booking(captain);
        SplitPaymentDtos.SplitShareRequest custom = share(1L, BigDecimal.valueOf(100));
        SplitPaymentDtos.CreateSplitRequest request = new SplitPaymentDtos.CreateSplitRequest();
        request.setBookingId(10L);
        request.setSplitType("CUSTOM");
        request.setShares(List.of(custom));

        when(userRepository.findByEmailOrPhone("captain@test.com")).thenReturn(Optional.of(captain));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(splitRepository.findByBookingIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(captain));

        ApiException error = assertThrows(ApiException.class, () -> service.createSplit("captain@test.com", request));

        assertEquals("Custom split amounts must equal the booking total.", error.getMessage());
    }

    @Test
    void payShareMarksSharePaidAndRecalculatesCollection() {
        User captain = user(1L, "Captain");
        User player = user(2L, "Player");
        Booking booking = booking(captain);
        PaymentSplit split = PaymentSplit.builder()
                .id(80L)
                .booking(booking)
                .user(player)
                .amount(BigDecimal.valueOf(1200))
                .status(PaymentSplitStatus.PENDING)
                .dueDate(LocalDate.now())
                .build();
        TeamCollection collection = TeamCollection.builder()
                .id(50L)
                .booking(booking)
                .captain(captain)
                .totalAmount(BigDecimal.valueOf(2400))
                .collectedAmount(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.valueOf(2400))
                .status(CollectionStatus.OPEN)
                .build();
        SplitPaymentDtos.PayShareRequest request = new SplitPaymentDtos.PayShareRequest();
        request.setSplitId(80L);
        request.setUseWallet(true);

        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(player));
        when(splitRepository.findById(80L)).thenReturn(Optional.of(split));
        when(splitRepository.save(any(PaymentSplit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(collectionRepository.findByBookingId(10L)).thenReturn(Optional.of(collection));
        when(splitRepository.findByBookingIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(split));
        when(collectionRepository.save(any(TeamCollection.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SplitPaymentDtos.SplitPaymentResponse response = service.payShare("player@test.com", request);

        assertEquals(PaymentSplitStatus.PAID, response.getSplits().get(0).getStatus());
        assertEquals(100, response.getCollection().getCollectionRate());
        verify(walletService).debitWallet(eq(player), eq(BigDecimal.valueOf(1200)), eq("SPLIT_PAYMENT"), anyString());
    }

    @Test
    void updateOwnerRulesStoresProductionDefaults() {
        User owner = user(1L, "Owner");
        SplitPaymentDtos.PaymentRuleRequest request = new SplitPaymentDtos.PaymentRuleRequest();
        request.setMinimumPercentage(70);
        request.setAutoCancel(true);
        request.setFullPaymentRequired(false);
        request.setReminderHoursBefore(2);

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(ruleRepository.findByOwnerId(1L)).thenReturn(Optional.empty());
        when(ruleRepository.save(any(PaymentRule.class))).thenAnswer(invocation -> {
            PaymentRule rule = invocation.getArgument(0);
            rule.setId(44L);
            return rule;
        });

        SplitPaymentDtos.PaymentRuleResponse response = service.updateOwnerRules("owner@test.com", request);

        assertEquals(70, response.getMinimumPercentage());
        assertTrue(response.getAutoCancel());
        assertEquals(2, response.getReminderHoursBefore());
    }

    private SplitPaymentDtos.SplitShareRequest share(Long userId, BigDecimal amount) {
        SplitPaymentDtos.SplitShareRequest share = new SplitPaymentDtos.SplitShareRequest();
        share.setUserId(userId);
        share.setAmount(amount);
        return share;
    }

    private Booking booking(User captain) {
        Turf turf = new Turf();
        turf.setId(20L);
        turf.setOwner(user(9L, "Owner"));
        turf.setName("Greenfield Arena");
        turf.setCity("Ahmedabad");
        turf.setLocation("Satellite");
        Slot slot = new Slot();
        slot.setId(30L);
        slot.setTurf(turf);
        slot.setStartTime(LocalTime.of(19, 0));
        slot.setEndTime(LocalTime.of(21, 0));
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setUser(captain);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setAmount(BigDecimal.valueOf(2400));
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@test.com");
        user.setPhone("900000000" + id);
        user.setRoles(new HashSet<>());
        return user;
    }
}
