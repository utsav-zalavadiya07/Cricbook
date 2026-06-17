package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingPreferenceRepository extends JpaRepository<BookingPreference, Long> {
    Optional<BookingPreference> findByUserId(Long userId);
}
