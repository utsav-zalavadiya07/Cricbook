package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BookingPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingPredictionRepository extends JpaRepository<BookingPrediction, Long> {
    List<BookingPrediction> findTop5ByUserIdOrderByConfidenceDescCreatedAtDesc(Long userId);
}
