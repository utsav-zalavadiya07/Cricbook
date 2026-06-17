package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingTimelineRepository extends JpaRepository<BookingTimeline, Long> {
    List<BookingTimeline> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    List<BookingTimeline> findTop100ByOrderByCreatedAtDesc();
}
