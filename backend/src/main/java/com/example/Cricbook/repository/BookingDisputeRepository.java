package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingDisputeRepository extends JpaRepository<BookingDispute, Long> {
    List<BookingDispute> findByStatusOrderByCreatedAtDesc(String status);
    List<BookingDispute> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
