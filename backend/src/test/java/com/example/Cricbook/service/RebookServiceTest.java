package com.example.Cricbook.service;

import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.RebookDtos.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.BookingTemplateStatus;
import com.example.Cricbook.enums.RecurringBookingFrequency;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RebookServiceTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingService bookingService;
    @Mock private RebookHistoryRepository rebookHistoryRepository;
    @Mock private RecurringBookingRepository recurringBookingRepository;
    @Mock private BookingTemplateRepository bookingTemplateRepository;
    @Mock private BookingPredictionRepository bookingPredictionRepository;
    @Mock private AutoWaitlistRepository autoWaitlistRepository;
    @Mock private BookingConflictRepository bookingConflictRepository;
    @Mock private UserRepository userRepository;
    @Mock private TurfRepository turfRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private NotificationService notificationService;

    private RebookService rebookService;
    private User user;
    private User owner;
    private Turf turf;
    private Slot slot;
    private Booking booking;

    @BeforeEach
    void setUp() {
        rebookService = new RebookService(
                bookingRepository,
                bookingService,
                rebookHistoryRepository,
                recurringBookingRepository,
                bookingTemplateRepository,
                bookingPredictionRepository,
                autoWaitlistRepository,
                bookingConflictRepository,
                userRepository,
                turfRepository,
                slotRepository,
                notificationService
        );

        user = User.builder().id(10L).email("player@test.com").phone("999").name("Player").build();
        owner = User.builder().id(20L).email("owner@test.com").phone("888").name("Owner").build();
        turf = Turf.builder().id(30L).name("Greenfield").city("Ahmedabad").location("SG Road").owner(owner).pricePerHour(BigDecimal.valueOf(1200)).build();
        slot = Slot.builder().id(40L).turf(turf).startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(22, 0)).price(BigDecimal.valueOf(1200)).isActive(true).build();
        booking = Booking.builder()
                .id(50L)
                .user(user)
                .turf(turf)
                .slot(slot)
                .bookingDate(LocalDate.now().minusWeeks(1))
                .status(BookingStatus.CONFIRMED)
                .amount(BigDecimal.valueOf(1200))
                .build();
    }

    @Test
    void rebookCreatesBookingAndTracksHistory() {
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.existsByUserIdAndBookingDateAndSlotIdAndStatusIn(eq(10L), any(), eq(40L), anyList())).thenReturn(false);
        when(bookingService.createBookingForUser(eq(user), eq(30L), eq(40L), any()))
                .thenReturn(BookingResponse.builder().id(99L).turfId(30L).slotId(40L).status(BookingStatus.PENDING).build());
        when(rebookHistoryRepository.findByUserIdAndSourceBookingId(10L, 50L)).thenReturn(Optional.empty());
        when(rebookHistoryRepository.save(any(RebookHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = rebookService.rebook("player@test.com", RebookRequest.builder().bookingId(50L).targetDate(LocalDate.now().plusWeeks(1)).build());

        assertEquals(99L, response.getId());
        verify(bookingService).createBookingForUser(eq(user), eq(30L), eq(40L), any());
        verify(rebookHistoryRepository).save(argThat(history -> history.getRebookCount() == 1));
        verify(notificationService).pushNotification(eq(10L), eq("player@test.com"), anyString(), anyString());
    }

    @Test
    void rebookRejectsAnotherUsersBooking() {
        User other = User.builder().id(99L).email("other@test.com").phone("777").build();
        booking.setUser(other);
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> rebookService.rebook("player@test.com", RebookRequest.builder().bookingId(50L).build()));

        assertEquals("You can only rebook your own bookings.", ex.getMessage());
        verify(bookingService, never()).createBookingForUser(any(), anyLong(), anyLong(), any());
    }

    @Test
    void frequentBookingsBuildsSuggestionsFromHistory() {
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findByUserIdOrderByBookingDateDescCreatedAtDesc(10L)).thenReturn(List.of(booking));

        List<FrequentBookingResponse> result = rebookService.frequentBookings("player@test.com");

        assertEquals(1, result.size());
        assertEquals("Greenfield", result.get(0).getTurfName());
        assertNotNull(result.get(0).getNextSuggestedDate());
    }

    @Test
    void createRecurringPersistsSchedule() {
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(turfRepository.findById(30L)).thenReturn(Optional.of(turf));
        when(slotRepository.findById(40L)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsByUserIdAndBookingDateAndSlotIdAndStatusIn(anyLong(), any(), anyLong(), anyList())).thenReturn(false);
        when(recurringBookingRepository.save(any(RecurringBooking.class))).thenAnswer(invocation -> {
            RecurringBooking item = invocation.getArgument(0);
            item.setId(70L);
            return item;
        });

        RecurringBookingResponse result = rebookService.createRecurring("player@test.com", RecurringBookingRequest.builder()
                .turfId(30L)
                .slotId(40L)
                .frequency(RecurringBookingFrequency.WEEKLY)
                .preferredDay(DayOfWeek.FRIDAY)
                .nextBookingDate(LocalDate.now().plusDays(7))
                .autoConfirm(false)
                .build());

        assertEquals(70L, result.getId());
        assertEquals(RecurringBookingFrequency.WEEKLY, result.getFrequency());
    }

    @Test
    void createTemplatePersistsActiveTemplate() {
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(turfRepository.findById(30L)).thenReturn(Optional.of(turf));
        when(bookingTemplateRepository.save(any(BookingTemplate.class))).thenAnswer(invocation -> {
            BookingTemplate item = invocation.getArgument(0);
            item.setId(80L);
            return item;
        });

        BookingTemplateResponse result = rebookService.createTemplate("player@test.com", BookingTemplateRequest.builder()
                .templateName("Friday Match")
                .turfId(30L)
                .preferredDay(DayOfWeek.FRIDAY)
                .preferredTime(LocalTime.of(20, 0))
                .players(12)
                .build());

        assertEquals(80L, result.getId());
        assertEquals(BookingTemplateStatus.ACTIVE.name(), result.getStatus());
    }

    @Test
    void createAutoWaitlistPersistsPreference() {
        when(userRepository.findByEmail("player@test.com")).thenReturn(Optional.of(user));
        when(slotRepository.findById(40L)).thenReturn(Optional.of(slot));
        when(autoWaitlistRepository.save(any(AutoWaitlist.class))).thenAnswer(invocation -> {
            AutoWaitlist item = invocation.getArgument(0);
            item.setId(90L);
            return item;
        });

        AutoWaitlistResponse result = rebookService.createAutoWaitlist("player@test.com", AutoWaitlistRequest.builder()
                .slotId(40L)
                .preferredDate(LocalDate.now().plusDays(3))
                .autoBookEnabled(true)
                .build());

        assertEquals(90L, result.getId());
        assertTrue(result.getAutoBookEnabled());
    }
}
