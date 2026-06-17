package com.example.Cricbook.service;

import com.example.Cricbook.dto.TravelExperienceRequests;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.*;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelExperienceServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TravelPartnerRepository partnerRepository;
    @Mock
    private TravelPackageRepository packageRepository;
    @Mock
    private TravelBookingRepository bookingRepository;
    @Mock
    private TravelEventRepository eventRepository;
    @Mock
    private TravelTicketTypeRepository ticketTypeRepository;
    @Mock
    private TravelTicketRepository ticketRepository;
    @Mock
    private TravelAccommodationRepository accommodationRepository;
    @Mock
    private TravelAccommodationBookingRepository accommodationBookingRepository;
    @Mock
    private TravelAuditRepository auditRepository;

    @InjectMocks
    private TravelExperienceService travelExperienceService;

    @Test
    void bookPackageAppliesCouponAndReducesCapacity() {
        User user = User.builder()
                .id(5L)
                .name("Player")
                .email("player@example.com")
                .phone("9999999999")
                .status(UserStatus.ACTIVE)
                .build();
        TravelPackage travelPackage = TravelPackage.builder()
                .id(11L)
                .packageCode("GOA-CAMP")
                .title("Goa Cricket Camp")
                .sport("Cricket")
                .originCity("Ahmedabad")
                .destination("Goa")
                .packageType("FULL_PACKAGE")
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(24))
                .cancellationDeadline(LocalDate.now().plusDays(10))
                .pricePerPerson(BigDecimal.valueOf(1000).setScale(2))
                .capacity(5)
                .bookedCount(2)
                .includesAccommodation(true)
                .includesTransport(true)
                .status(TravelPackageStatus.PUBLISHED)
                .build();

        when(userRepository.findByEmailOrPhone("player@example.com")).thenReturn(Optional.of(user));
        when(packageRepository.findById(11L)).thenReturn(Optional.of(travelPackage));
        when(bookingRepository.save(any(TravelBooking.class))).thenAnswer(invocation -> {
            TravelBooking booking = invocation.getArgument(0);
            booking.setId(31L);
            booking.setBookingReference("TRV-TEST");
            return booking;
        });
        when(auditRepository.save(any(TravelAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TravelExperienceRequests.PackageBookingRequest request = new TravelExperienceRequests.PackageBookingRequest();
        request.setParticipantCount(2);
        request.setBookingType("TEAM");
        request.setCouponCode("TRAVEL10");
        request.setPaymentReference("PAY-1");

        Map<String, Object> response = travelExperienceService.bookPackage(11L, request, "player@example.com");

        assertEquals("TRV-TEST", response.get("bookingReference"));
        assertEquals(TravelBookingStatus.CONFIRMED, response.get("status"));
        assertEquals(4, travelPackage.getBookedCount());
        assertEquals(0, BigDecimal.valueOf(1800).setScale(2).compareTo((BigDecimal) response.get("total")));
        verify(packageRepository).save(travelPackage);
        verify(auditRepository).save(any(TravelAudit.class));
    }

    @Test
    void validateTicketQrMarksUsedAndRejectsReuse() {
        User user = User.builder()
                .id(6L)
                .name("Fan")
                .email("fan@example.com")
                .phone("8888888888")
                .status(UserStatus.ACTIVE)
                .build();
        TravelEvent event = TravelEvent.builder()
                .id(41L)
                .title("Sports Festival")
                .eventType(TravelEventType.FESTIVAL)
                .sport("Cricket")
                .city("Ahmedabad")
                .startsAt(LocalDateTime.now().plusDays(1))
                .endsAt(LocalDateTime.now().plusDays(2))
                .capacity(100)
                .registeredCount(1)
                .basePrice(BigDecimal.valueOf(500).setScale(2))
                .status(TravelEventStatus.PUBLISHED)
                .build();
        TravelTicketType ticketType = TravelTicketType.builder()
                .id(51L)
                .event(event)
                .name("General")
                .price(BigDecimal.valueOf(500).setScale(2))
                .totalInventory(100)
                .soldInventory(1)
                .active(true)
                .build();
        TravelTicket ticket = TravelTicket.builder()
                .id(61L)
                .event(event)
                .ticketType(ticketType)
                .user(user)
                .qrCode("TRV-QR-1")
                .quantity(1)
                .amount(BigDecimal.valueOf(500).setScale(2))
                .status(TravelTicketStatus.ISSUED)
                .build();

        when(ticketRepository.findByQrCode("TRV-QR-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(TravelTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRepository.save(any(TravelAudit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = travelExperienceService.validateTicketQr("TRV-QR-1", "admin@example.com");

        assertEquals(TravelTicketStatus.USED, response.get("status"));
        assertNotNull(ticket.getUsedAt());
        assertThrows(ApiException.class, () -> travelExperienceService.validateTicketQr("TRV-QR-1", "admin@example.com"));
    }
}
