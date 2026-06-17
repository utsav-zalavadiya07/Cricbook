package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CancellationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CancellationHistoryRepository extends JpaRepository<CancellationHistory, Long> {
    Optional<CancellationHistory> findByBookingId(Long bookingId);
    long countByUserId(Long userId);
}
