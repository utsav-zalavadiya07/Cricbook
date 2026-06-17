package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingModificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingModificationLogRepository extends JpaRepository<BookingModificationLog, Long> {
    List<BookingModificationLog> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
