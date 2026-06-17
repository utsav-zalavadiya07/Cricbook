package com.example.Cricbook.service;

import com.example.Cricbook.dto.OwnerFinanceDashboardResponse;
import com.example.Cricbook.entity.*;
import com.example.Cricbook.enums.BookingStatus;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerFinanceServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private OwnerSettlementRepository ownerSettlementRepository;
    @Mock private CommissionRuleRepository commissionRuleRepository;
    @Mock private FinanceAuditRepository financeAuditRepository;
    @InjectMocks private OwnerFinanceService service;

    @Test
    void dashboardCalculatesOwnerRevenueCommissionTaxRefundsAndTurfBreakdown() {
        User owner = user(1L, "Owner");
        User player = user(2L, "Captain");
        Turf turf = turf(10L, owner, "Prime Arena");
        Payment payment = payment(100L, booking(200L, player, turf, BigDecimal.valueOf(3000)), "SUCCESS", BigDecimal.valueOf(3000), LocalDateTime.now());
        Refund refund = Refund.builder().id(300L).payment(payment).amount(BigDecimal.valueOf(500)).status("SUCCESS").reason("Weather").build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(paymentRepository.findByBookingTurfOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(payment));
        when(refundRepository.findByPaymentBookingTurfOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(refund));
        when(ownerSettlementRepository.findByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(commissionRuleRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(commissionRule(null, 10)));

        OwnerFinanceDashboardResponse response = service.getDashboard("owner@test.com", LocalDate.now().minusDays(1), LocalDate.now(), null);

        assertEquals(0, BigDecimal.valueOf(3000).setScale(2).compareTo(response.getGrossRevenue()));
        assertEquals(0, BigDecimal.valueOf(500).setScale(2).compareTo(response.getRefundedAmount()));
        assertEquals(0, BigDecimal.valueOf(2500).setScale(2).compareTo(response.getNetCollectedRevenue()));
        assertEquals(0, BigDecimal.valueOf(250).setScale(2).compareTo(response.getCommissionDue()));
        assertEquals(0, BigDecimal.valueOf(45).setScale(2).compareTo(response.getGstOnCommission()));
        assertEquals(0, BigDecimal.valueOf(2205).setScale(2).compareTo(response.getNetPayable()));
        assertEquals(1, response.getPaidBookings());
        assertEquals("Prime Arena", response.getTurfRevenue().get(0).getTurfName());
        assertEquals(0, BigDecimal.valueOf(2500).setScale(2).compareTo(response.getTurfRevenue().get(0).getRevenue()));
    }

    @Test
    void generateSettlementsCreatesOnlyMissingSuccessfulPaymentSettlements() {
        User owner = user(1L, "Owner");
        User player = user(2L, "Captain");
        Turf turf = turf(10L, owner, "Prime Arena");
        Payment successful = payment(100L, booking(200L, player, turf, BigDecimal.valueOf(2000)), "SUCCESS", BigDecimal.valueOf(2000), LocalDateTime.now());
        Payment failed = payment(101L, booking(201L, player, turf, BigDecimal.valueOf(1500)), "FAILED", BigDecimal.valueOf(1500), LocalDateTime.now());
        Refund refund = Refund.builder().id(300L).payment(successful).amount(BigDecimal.valueOf(500)).status("SUCCESS").reason("Partial refund").build();

        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));
        when(paymentRepository.findByBookingTurfOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(successful, failed));
        when(refundRepository.findByPaymentBookingTurfOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(refund));
        when(ownerSettlementRepository.findByBookingId(200L)).thenReturn(Optional.empty());
        when(ownerSettlementRepository.findByOwnerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(commissionRuleRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(commissionRule(null, 10)));
        when(ownerSettlementRepository.save(any(OwnerSettlement.class))).thenAnswer(invocation -> {
            OwnerSettlement settlement = invocation.getArgument(0);
            settlement.setId(900L);
            return settlement;
        });

        service.generateSettlements("owner@test.com");

        ArgumentCaptor<OwnerSettlement> captor = ArgumentCaptor.forClass(OwnerSettlement.class);
        verify(ownerSettlementRepository).save(captor.capture());
        OwnerSettlement saved = captor.getValue();
        assertEquals(0, BigDecimal.valueOf(1500).setScale(2).compareTo(saved.getGrossAmount()));
        assertEquals(0, BigDecimal.valueOf(150).setScale(2).compareTo(saved.getCommission()));
        assertEquals(0, BigDecimal.valueOf(1323).setScale(2).compareTo(saved.getNetAmount()));
        assertEquals("PENDING", saved.getStatus());
        verify(ownerSettlementRepository, never()).findByBookingId(201L);
        verify(financeAuditRepository).save(any(FinanceAudit.class));
    }

    @Test
    void dashboardRejectsInvalidFutureDateRange() {
        User owner = user(1L, "Owner");
        when(userRepository.findByEmailOrPhone("owner@test.com")).thenReturn(Optional.of(owner));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.getDashboard("owner@test.com", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), null));

        assertEquals("Future date ranges are not allowed for financial reports.", exception.getMessage());
        verifyNoInteractions(paymentRepository);
    }

    private CommissionRule commissionRule(Long ownerId, int rate) {
        return CommissionRule.builder()
                .type("PERCENT")
                .ownerId(ownerId)
                .value(BigDecimal.valueOf(rate))
                .active(true)
                .description("Test rule")
                .build();
    }

    private Payment payment(Long id, Booking booking, String status, BigDecimal amount, LocalDateTime createdAt) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setBooking(booking);
        payment.setStatus(status);
        payment.setAmount(amount);
        payment.setCreatedAt(createdAt);
        return payment;
    }

    private Booking booking(Long id, User player, Turf turf, BigDecimal amount) {
        Slot slot = Slot.builder()
                .id(30L + id)
                .turf(turf)
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(8, 0))
                .price(amount)
                .isActive(true)
                .build();
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(player);
        booking.setTurf(turf);
        booking.setSlot(slot);
        booking.setBookingDate(LocalDate.now());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAmount(amount);
        return booking;
    }

    private Turf turf(Long id, User owner, String name) {
        Turf turf = new Turf();
        turf.setId(id);
        turf.setOwner(owner);
        turf.setName(name);
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
