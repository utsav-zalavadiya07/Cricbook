package com.example.Cricbook.repository;

import com.example.Cricbook.entity.NoShowCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoShowCaseRepository extends JpaRepository<NoShowCase, Long> {
    List<NoShowCase> findByStatusOrderByCreatedAtDesc(String status);
    Optional<NoShowCase> findByBookingId(Long bookingId);
}
