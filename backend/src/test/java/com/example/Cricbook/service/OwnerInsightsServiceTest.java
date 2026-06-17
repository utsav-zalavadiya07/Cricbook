package com.example.Cricbook.service;

import com.example.Cricbook.dto.OwnerExpenseRequest;
import com.example.Cricbook.dto.OwnerExpenseResponse;
import com.example.Cricbook.dto.OwnerInsightsResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerInsightsServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private OwnerExpenseRepository expenseRepository;
    @Mock private PlayerTrustScoreRepository trustScoreRepository;
    @Mock private TeamRepository teamRepository;
    @InjectMocks private OwnerInsightsService service;

    @Test
    void ownerInsightsBuildsRevenueOccupancyCustomersAndProfit() {
        User owner = user(1L, "Owner");
        User player = user(2L, "Captain");
        Turf turf = turf(owner);
        Slot evening = slot(turf, 20L, 19, BigDecimal.valueOf(1000));
        Booking today = booking(100L, player, turf, evening, LocalDate.now(), BigDecimal.valueOf(1000), BookingStatus.CONFIRMED);
        Booking lastWeek = booking(101L, player, turf, evening, LocalDate.now().minusDays(8), BigDecimal.valueOf(800), BookingStatus.CONFIRMED);
        BigDecimal expectedMonthRevenue = lastWeek.getBookingDate().getMonth().equals(LocalDate.now().getMonth())
                ? BigDecimal.valueOf(1800)
                : BigDecimal.valueOf(1000);

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(turfRepository.findByOwnerId(1L)).thenReturn(List.of(turf));
        when(bookingRepository.findByTurfOwnerId(1L)).thenReturn(List.of(today, lastWeek));
        when(slotRepository.findByTurfId(10L)).thenReturn(List.of(evening));
        when(expenseRepository.sumByOwnerAndDateRange(eq(1L), any(), any())).thenReturn(BigDecimal.valueOf(250));
        when(expenseRepository.findByOwnerIdAndExpenseDateBetweenOrderByExpenseDateDesc(eq(1L), any(), any())).thenReturn(List.of());
        when(trustScoreRepository.findByUserId(2L)).thenReturn(Optional.of(PlayerTrustScore.builder().score(92).build()));
        when(teamRepository.findByCaptainIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        OwnerInsightsResponse result = service.getOwnerInsights("owner@test.com");

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.getRevenue().getTodayRevenue()));
        assertEquals(0, expectedMonthRevenue.compareTo(result.getRevenue().getMonthRevenue()));
        assertEquals(0, BigDecimal.valueOf(1550).compareTo(result.getProfit().getProfit()));
        assertEquals(92, result.getCustomers().get(0).getTrustScore());
        assertFalse(result.getHeatmap().isEmpty());
        assertEquals("7 PM", result.getHeatmap().get(0).getHourLabel());
        assertFalse(result.getPricingSuggestions().isEmpty());
    }

    @Test
    void addExpenseValidatesAndPersistsOwnerExpense() {
        User owner = user(1L, "Owner");
        OwnerExpenseRequest request = new OwnerExpenseRequest();
        request.setType("Electricity");
        request.setAmount(BigDecimal.valueOf(500));
        request.setExpenseDate(LocalDate.now());
        request.setNote("Monthly bill");

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(expenseRepository.save(any(OwnerExpense.class))).thenAnswer(invocation -> {
            OwnerExpense expense = invocation.getArgument(0);
            expense.setId(88L);
            return expense;
        });

        OwnerExpenseResponse result = service.addExpense("owner@test.com", request);

        assertEquals(88L, result.getId());
        assertEquals("Electricity", result.getType());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.getAmount()));
    }

    @Test
    void coachReturnsPricingSpecificActions() {
        User owner = user(1L, "Owner");
        Turf turf = turf(owner);
        Slot morning = slot(turf, 20L, 8, BigDecimal.valueOf(700));
        OwnerInsightsResponse.BusinessCoachRequest request = OwnerInsightsResponse.BusinessCoachRequest.builder()
                .question("What pricing should I use?")
                .build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(turfRepository.findByOwnerId(1L)).thenReturn(List.of(turf));
        when(bookingRepository.findByTurfOwnerId(1L)).thenReturn(List.of());
        when(slotRepository.findByTurfId(10L)).thenReturn(List.of(morning));
        when(expenseRepository.sumByOwnerAndDateRange(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.findByOwnerIdAndExpenseDateBetweenOrderByExpenseDateDesc(eq(1L), any(), any())).thenReturn(List.of());
        when(teamRepository.findByCaptainIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        OwnerInsightsResponse.BusinessCoachResponse response = service.askCoach("owner@test.com", request);

        assertTrue(response.getRecommendedActions().get(0).contains("slot-level pricing"));
        assertTrue(response.getAnswer().contains("occupancy"));
    }

    private Booking booking(Long id, User user, Turf turf, Slot slot, LocalDate date, BigDecimal amount, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(date);
        booking.setAmount(amount);
        booking.setStatus(status);
        return booking;
    }

    private Slot slot(Turf turf, Long id, int hour, BigDecimal price) {
        Slot slot = new Slot();
        slot.setId(id);
        slot.setTurf(turf);
        slot.setStartTime(LocalTime.of(hour, 0));
        slot.setEndTime(LocalTime.of(hour + 1, 0));
        slot.setPrice(price);
        slot.setIsActive(true);
        return slot;
    }

    private Turf turf(User owner) {
        Turf turf = new Turf();
        turf.setId(10L);
        turf.setOwner(owner);
        turf.setName("Greenfield Arena");
        turf.setCity("Ahmedabad");
        turf.setLocation("Satellite");
        turf.setPricePerHour(BigDecimal.valueOf(1000));
        return turf;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@test.com");
        user.setPhone("900000000" + id);
        return user;
    }
}
