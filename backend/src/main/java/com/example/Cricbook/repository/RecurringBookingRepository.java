package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RecurringBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, Long> {
    List<RecurringBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<RecurringBooking> findByTurfOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
