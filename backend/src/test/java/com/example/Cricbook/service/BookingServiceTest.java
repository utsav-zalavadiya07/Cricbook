package com.example.Cricbook.service;

import com.example.Cricbook.dto.BookingResponse;
import com.example.Cricbook.dto.CreateBookingRequest;
import com.example.Cricbook.dto.OwnerBookingActionRequest;
import com.example.Cricbook.dto.OwnerBookingOperationsSummary;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.AttendanceStatus;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private SlotRepository slotRepository;
    @Mock
    private SlotAvailabilityRepository slotAvailabilityRepository;
    @Mock
    private TurfRepository turfRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SlotPriceOverrideRepository slotPriceOverrideRepository;
    @Mock
    private UserMembershipRepository userMembershipRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TrustAttendanceRepository trustAttendanceRepository;
    @Mock
    private BookingTimelineRepository bookingTimelineRepository;
    @Mock
    private NoShowCaseRepository noShowCaseRepository;
    @Mock
    private BookingCancellationRequestRepository bookingCancellationRequestRepository;
    @Mock
    private MonetizationGateService monetizationGateService;
    @Mock
    private LoyaltyService loyaltyService;
    @Mock
    private TrustScoreService trustScoreService;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookingService bookingService;

    private User mockUser;
    private Turf mockTurf;
    private Slot mockSlot;
    private SlotAvailability mockSa;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");

        mockTurf = new Turf();
        mockTurf.setId(1L);
        mockTurf.setName("Test Turf");

        mockSlot = new Slot();
        mockSlot.setId(1L);
        mockSlot.setTurf(mockTurf);
        mockSlot.setPrice(new BigDecimal("1000.00"));
        mockSlot.setStartTime(LocalTime.of(7, 0));
        mockSlot.setEndTime(LocalTime.of(8, 0));
        mockSlot.setIsActive(true);

        mockSa = new SlotAvailability();
        mockSa.setId(1L);
        mockSa.setSlot(mockSlot);
        mockSa.setIsAvailable(true);
        mockSa.setIsBlocked(false);
    }

    @Test
    void testCreateBooking_Success() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTurfId(1L);
        request.setSlotId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@test.com");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(turfRepository.findById(1L)).thenReturn(Optional.of(mockTurf));
        when(slotAvailabilityRepository.lockSlot(1L, request.getBookingDate())).thenReturn(Optional.of(mockSa));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(mockSlot));
        when(slotPriceOverrideRepository.findBySlotIdAndDate(1L, request.getBookingDate())).thenReturn(Optional.empty());
        when(subscriptionRepository.findFirstActiveSubscription(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(userMembershipRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(eq(1L), any(), any()))
                .thenReturn(Optional.empty());

        Booking mockBooking = new Booking();
        mockBooking.setId(1L);
        mockBooking.setUser(mockUser);
        mockBooking.setTurf(mockTurf);
        mockBooking.setSlot(mockSlot);
        mockBooking.setBookingDate(request.getBookingDate());
        mockBooking.setStatus(BookingStatus.PENDING);
        mockBooking.setAmount(new BigDecimal("1000.00"));

        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        // Act
        BookingResponse response = bookingService.createBooking(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertFalse(mockSa.getIsAvailable());
        verify(slotAvailabilityRepository).save(mockSa);
    }

    @Test
    void testCreateBooking_SlotNotAvailable() {
        // Arrange
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTurfId(1L);
        request.setSlotId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@test.com");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(turfRepository.findById(1L)).thenReturn(Optional.of(mockTurf));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(mockSlot));
        
        mockSa.setIsAvailable(false);
        when(slotAvailabilityRepository.lockSlot(1L, request.getBookingDate())).thenReturn(Optional.of(mockSa));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(request);
        });

        assertEquals("Selected slot is no longer available.", exception.getMessage());
    }

    @Test
    void testConfirmBooking_Success() {
        Long bookingId = 1L;
        com.example.Cricbook.dto.ConfirmBookingRequest request = com.example.Cricbook.dto.ConfirmBookingRequest.builder()
                .paymentId("pay_123")
                .paymentStatus("SUCCESS")
                .build();
        
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        
        Turf turf = new Turf();
        turf.setId(1L);

        Slot slot = new Slot();
        slot.setId(1L);
        slot.setStartTime(LocalTime.of(7, 0));
        slot.setEndTime(LocalTime.of(8, 0));
        
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(LocalDate.now());
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        
        BookingResponse response = bookingService.confirmBooking(bookingId, request);
        
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(notificationService).pushNotification(any(), any(), any(), any());
    }

    @Test
    void testCancelBooking_Success() {
        Long bookingId = 1L;
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        
        Turf turf = new Turf();
        turf.setId(1L);

        Slot slot = new Slot();
        slot.setId(1L);
        slot.setStartTime(LocalTime.of(7, 0));
        slot.setEndTime(LocalTime.of(8, 0));
        
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(LocalDate.now().plusDays(2));
        
        SlotAvailability sa = new SlotAvailability();
        sa.setSlot(slot);
        sa.setDate(booking.getBookingDate());
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(slotAvailabilityRepository.findBySlotIdAndDate(eq(1L), any())).thenReturn(Optional.of(sa));
        
        BookingResponse response = bookingService.cancelBooking(bookingId);
        
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertTrue(sa.getIsAvailable());
        verify(notificationService).pushNotification(any(), any(), any(), any());
    }

    @Test
    void testCreateBooking_RejectsPastDate() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTurfId(1L);
        request.setSlotId(1L);
        request.setBookingDate(LocalDate.now().minusDays(1));

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request));

        assertEquals("Booking date cannot be in the past.", exception.getMessage());
        verifyNoInteractions(slotAvailabilityRepository);
    }

    @Test
    void testCreateBooking_RejectsInactiveSlot() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTurfId(1L);
        request.setSlotId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        mockSlot.setIsActive(false);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(turfRepository.findById(1L)).thenReturn(Optional.of(mockTurf));
        when(slotRepository.findById(1L)).thenReturn(Optional.of(mockSlot));

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.createBooking(request));

        assertEquals("Selected slot is not active.", exception.getMessage());
        verify(slotAvailabilityRepository, never()).lockSlot(any(), any());
    }

    @Test
    void testCancelBooking_RejectsConfirmedBookingInsideCutoff() {
        Long bookingId = 1L;
        User user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        Turf turf = new Turf();
        turf.setId(1L);
        Slot slot = new Slot();
        slot.setId(1L);
        slot.setStartTime(LocalTime.now().plusHours(2));
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUser(user);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(LocalDate.now());

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.cancelBooking(bookingId));

        assertEquals("Confirmed bookings can only be cancelled at least 24 hours before the slot starts.", exception.getMessage());
        verify(slotAvailabilityRepository, never()).save(any());
    }

    @Test
    void testOwnerCheckInConfirmedBooking() {
        User owner = owner();
        Booking booking = ownerBooking(owner, BookingStatus.CONFIRMED, LocalDate.now(), LocalTime.now().plusMinutes(30), LocalTime.now().plusMinutes(90));
        OwnerBookingActionRequest request = new OwnerBookingActionRequest();
        request.setNote("Player arrived.");

        authenticate("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(trustAttendanceRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(trustAttendanceRepository.save(any(TrustAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingTimelineRepository.save(any(BookingTimeline.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        BookingResponse response = bookingService.checkInOwnerBooking(10L, request);

        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertEquals(BookingStatus.CHECKED_IN, response.getStatus());
        assertEquals("ARRIVED", response.getAttendanceStatus());
        verify(trustAttendanceRepository).save(any(TrustAttendance.class));
        verify(bookingTimelineRepository).save(any(BookingTimeline.class));
    }

    @Test
    void testOwnerNoShowConfirmedPastBooking() {
        User owner = owner();
        Booking booking = ownerBooking(owner, BookingStatus.CONFIRMED, LocalDate.now(), LocalTime.now().minusHours(2), LocalTime.now().minusHours(1));
        OwnerBookingActionRequest request = new OwnerBookingActionRequest();
        request.setReason("Player did not arrive.");

        authenticate("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(trustAttendanceRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(trustAttendanceRepository.save(any(TrustAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(noShowCaseRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(noShowCaseRepository.save(any(NoShowCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingTimelineRepository.save(any(BookingTimeline.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        BookingResponse response = bookingService.markOwnerNoShow(10L, request);

        assertEquals(BookingStatus.NO_SHOW, booking.getStatus());
        assertEquals(BookingStatus.NO_SHOW, response.getStatus());
        assertEquals("NO_SHOW", response.getAttendanceStatus());
        verify(noShowCaseRepository).save(any(NoShowCase.class));
    }

    @Test
    void testOwnerCannotManageAnotherOwnerBooking() {
        User owner = owner();
        User otherOwner = new User();
        otherOwner.setId(99L);
        otherOwner.setEmail("other-owner@test.com");
        Booking booking = ownerBooking(otherOwner, BookingStatus.CONFIRMED, LocalDate.now(), LocalTime.now().plusMinutes(30), LocalTime.now().plusMinutes(90));

        authenticate("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        Exception exception = assertThrows(RuntimeException.class, () -> bookingService.checkInOwnerBooking(10L, null));

        assertEquals("You do not have permission to manage this booking.", exception.getMessage());
        verify(trustAttendanceRepository, never()).save(any());
    }

    @Test
    void testOwnerOperationsSummaryCountsLifecycle() {
        User owner = owner();
        Booking confirmed = ownerBooking(owner, BookingStatus.CONFIRMED, LocalDate.now(), LocalTime.now().plusHours(1), LocalTime.now().plusHours(2));
        Booking checkedIn = ownerBooking(owner, BookingStatus.CHECKED_IN, LocalDate.now(), LocalTime.now().minusMinutes(30), LocalTime.now().plusMinutes(30));
        checkedIn.setId(11L);
        Booking completed = ownerBooking(owner, BookingStatus.COMPLETED, LocalDate.now().minusDays(1), LocalTime.of(7, 0), LocalTime.of(8, 0));
        completed.setId(12L);
        Booking noShow = ownerBooking(owner, BookingStatus.NO_SHOW, LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        noShow.setId(13L);
        checkedIn.setAmount(BigDecimal.valueOf(1000));
        confirmed.setAmount(BigDecimal.valueOf(1200));

        TrustAttendance attendance = TrustAttendance.builder().booking(checkedIn).user(checkedIn.getUser()).status(AttendanceStatus.ARRIVED).checkinTime(java.time.LocalDateTime.now()).build();

        authenticate("owner@test.com");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner));
        when(bookingRepository.findByTurfOwnerId(owner.getId())).thenReturn(java.util.List.of(confirmed, checkedIn, completed, noShow));
        when(trustAttendanceRepository.findByBookingId(10L)).thenReturn(Optional.empty());
        when(trustAttendanceRepository.findByBookingId(11L)).thenReturn(Optional.of(attendance));
        when(trustAttendanceRepository.findByBookingId(12L)).thenReturn(Optional.empty());
        when(trustAttendanceRepository.findByBookingId(13L)).thenReturn(Optional.empty());
        when(bookingTimelineRepository.findByBookingIdOrderByCreatedAtDesc(anyLong())).thenReturn(java.util.List.of());

        OwnerBookingOperationsSummary summary = bookingService.getOwnerOperationsSummary();

        assertEquals(4L, summary.getTotalBookings());
        assertEquals(2L, summary.getTodayBookings());
        assertEquals(1L, summary.getPendingArrivals());
        assertEquals(1L, summary.getCheckedInToday());
        assertEquals(1L, summary.getNoShowBookings());
        assertEquals(BigDecimal.valueOf(2200), summary.getTodayRevenue());
    }

    private void authenticate(String username) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn(username);
    }

    private User owner() {
        User owner = new User();
        owner.setId(2L);
        owner.setEmail("owner@test.com");
        owner.setName("Owner");
        owner.setPhone("9999999999");
        return owner;
    }

    private Booking ownerBooking(User owner, BookingStatus status, LocalDate date, LocalTime startTime, LocalTime endTime) {
        User player = new User();
        player.setId(1L);
        player.setEmail("player@test.com");
        player.setName("Player");
        player.setPhone("8888888888");

        Turf turf = new Turf();
        turf.setId(1L);
        turf.setName("Owner Turf");
        turf.setOwner(owner);

        Slot slot = new Slot();
        slot.setId(1L);
        slot.setTurf(turf);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);

        return Booking.builder()
                .id(10L)
                .user(player)
                .turf(turf)
                .slot(slot)
                .bookingDate(date)
                .status(status)
                .amount(BigDecimal.valueOf(1000))
                .build();
    }
}
