package com.example.Cricbook.service;

import com.example.Cricbook.dto.*;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.enums.ResaleListingStatus;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResaleServiceTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResaleListingRepository listingRepository;
    @Mock private BookingTransferRepository transferRepository;
    @Mock private ResaleRuleRepository ruleRepository;
    @Mock private SlotWaitlistRepository waitlistRepository;
    @Mock private ResaleTransactionRepository transactionRepository;
    @Mock private NotificationService notificationService;
    @Mock private TrustScoreService trustScoreService;
    @InjectMocks private ResaleService resaleService;

    @Test
    void createListingValidatesConfirmedOwnerBookingAndCommission() {
        User seller = user(1L, "Seller");
        Booking booking = booking(10L, seller, BookingStatus.CONFIRMED);
        CreateResaleListingRequest request = new CreateResaleListingRequest();
        request.setBookingId(10L);
        request.setPrice(BigDecimal.valueOf(900));

        when(userRepository.findByEmailOrPhone("seller@test.com")).thenReturn(Optional.of(seller));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(listingRepository.findByBookingIdAndStatus(10L, ResaleListingStatus.ACTIVE)).thenReturn(Optional.empty());
        when(ruleRepository.findByOwnerId(99L)).thenReturn(Optional.empty());
        when(trustScoreService.getProfile(1L)).thenReturn(TrustScoreResponse.builder().score(88).build());
        when(waitlistRepository.findBySlotIdAndDateAndActiveTrueOrderByPriorityDescCreatedAtAsc(20L, booking.getBookingDate())).thenReturn(List.of());
        when(listingRepository.save(any(ResaleListing.class))).thenAnswer(invocation -> {
            ResaleListing listing = invocation.getArgument(0);
            listing.setId(77L);
            return listing;
        });

        ResaleListingResponse result = resaleService.createListing("seller@test.com", request);

        assertEquals(77L, result.getId());
        assertEquals(ResaleListingStatus.ACTIVE, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(45).compareTo(result.getCommissionAmount()));
        assertEquals(10L, result.getBookingId());
    }

    @Test
    void createListingRejectsMarkupAboveOwnerRule() {
        User seller = user(1L, "Seller");
        Booking booking = booking(10L, seller, BookingStatus.CONFIRMED);
        CreateResaleListingRequest request = new CreateResaleListingRequest();
        request.setBookingId(10L);
        request.setPrice(BigDecimal.valueOf(1200));

        when(userRepository.findByEmailOrPhone("seller@test.com")).thenReturn(Optional.of(seller));
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(listingRepository.findByBookingIdAndStatus(10L, ResaleListingStatus.ACTIVE)).thenReturn(Optional.empty());
        when(ruleRepository.findByOwnerId(99L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> resaleService.createListing("seller@test.com", request));

        assertEquals("Resale price cannot exceed the owner markup rule.", error.getMessage());
    }

    @Test
    void buyListingTransfersBookingOwnershipAndRecordsTransaction() {
        User seller = user(1L, "Seller");
        User buyer = user(2L, "Buyer");
        Booking booking = booking(10L, seller, BookingStatus.CONFIRMED);
        ResaleListing listing = ResaleListing.builder()
                .id(77L)
                .booking(booking)
                .seller(seller)
                .price(BigDecimal.valueOf(900))
                .originalAmount(BigDecimal.valueOf(1000))
                .commissionAmount(BigDecimal.valueOf(45))
                .status(ResaleListingStatus.ACTIVE)
                .expiresAt(java.time.LocalDateTime.now().plusHours(3))
                .build();

        when(userRepository.findByEmailOrPhone("buyer@test.com")).thenReturn(Optional.of(buyer));
        when(listingRepository.findById(77L)).thenReturn(Optional.of(listing));
        when(ruleRepository.findByOwnerId(99L)).thenReturn(Optional.empty());
        when(trustScoreService.getProfile(2L)).thenReturn(TrustScoreResponse.builder().score(91).build());
        when(transactionRepository.existsByListingId(77L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(listingRepository.save(any(ResaleListing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(BookingTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(ResaleTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResaleListingResponse result = resaleService.buyListing("buyer@test.com", 77L, new BuyResaleListingRequest());

        assertEquals(ResaleListingStatus.SOLD, result.getStatus());
        assertEquals(2L, booking.getUser().getId());
        verify(transactionRepository).save(any(ResaleTransaction.class));
        verify(transferRepository).save(any(BookingTransfer.class));
    }

    @Test
    void joinWaitlistCreatesActivePriorityEntry() {
        User player = user(3L, "Player");
        Slot slot = slot();
        JoinWaitlistRequest request = new JoinWaitlistRequest();
        request.setSlotId(20L);
        request.setDate(LocalDate.now().plusDays(1));
        request.setPriority(7);

        when(userRepository.findByEmailOrPhone("player@test.com")).thenReturn(Optional.of(player));
        when(slotRepository.findById(20L)).thenReturn(Optional.of(slot));
        when(waitlistRepository.findBySlotIdAndUserIdAndDate(20L, 3L, request.getDate())).thenReturn(Optional.empty());
        when(waitlistRepository.save(any(SlotWaitlist.class))).thenAnswer(invocation -> {
            SlotWaitlist item = invocation.getArgument(0);
            item.setId(44L);
            return item;
        });

        SlotWaitlistResponse result = resaleService.joinWaitlist("player@test.com", request);

        assertEquals(44L, result.getId());
        assertTrue(result.getActive());
        assertEquals(7, result.getPriority());
    }

    private Booking booking(Long id, User user, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setTurf(turf());
        booking.setSlot(slot());
        booking.setBookingDate(LocalDate.now().plusDays(1));
        booking.setStatus(status);
        booking.setAmount(BigDecimal.valueOf(1000));
        return booking;
    }

    private Slot slot() {
        Slot slot = new Slot();
        slot.setId(20L);
        slot.setTurf(turf());
        slot.setStartTime(LocalTime.now().plusHours(4).withMinute(0).withSecond(0).withNano(0));
        slot.setEndTime(LocalTime.now().plusHours(6).withMinute(0).withSecond(0).withNano(0));
        slot.setPrice(BigDecimal.valueOf(1000));
        slot.setIsActive(true);
        return slot;
    }

    private Turf turf() {
        User owner = user(99L, "Owner");
        Turf turf = new Turf();
        turf.setId(30L);
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
