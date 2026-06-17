package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingEscalationCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEscalationCaseRepository extends JpaRepository<BookingEscalationCase, Long> {
    List<BookingEscalationCase> findByStatusOrderByCreatedAtDesc(String status);
}
