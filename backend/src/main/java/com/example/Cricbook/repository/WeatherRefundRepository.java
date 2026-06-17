package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WeatherRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherRefundRepository extends JpaRepository<WeatherRefund, Long> {
    List<WeatherRefund> findByBookingUserIdOrderByCreatedAtDesc(Long userId);
    long countByBookingTurfOwnerId(Long ownerId);
}
