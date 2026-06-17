package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingConflict;
import com.example.Cricbook.enums.BookingConflictStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingConflictRepository extends JpaRepository<BookingConflict, Long> {
    List<BookingConflict> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BookingConflictStatus status);
}
