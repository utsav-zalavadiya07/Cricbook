package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingCancellationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingCancellationRequestRepository extends JpaRepository<BookingCancellationRequest, Long> {
    List<BookingCancellationRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<BookingCancellationRequest> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
