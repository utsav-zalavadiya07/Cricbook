package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PaymentSplit;
import com.example.Cricbook.enums.PaymentSplitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaymentSplitRepository extends JpaRepository<PaymentSplit, Long> {
    List<PaymentSplit> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
    List<PaymentSplit> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PaymentSplit> findByBooking_Turf_Owner_IdOrderByCreatedAtDesc(Long ownerId);
    Optional<PaymentSplit> findByBookingIdAndUserId(Long bookingId, Long userId);
    long countByStatus(PaymentSplitStatus status);
    List<PaymentSplit> findByStatusAndDueDateBefore(PaymentSplitStatus status, LocalDate dueDate);
}
