package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WeatherReschedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherRescheduleRepository extends JpaRepository<WeatherReschedule, Long> {
    List<WeatherReschedule> findByBookingUserIdOrderByCreatedAtDesc(Long userId);
    List<WeatherReschedule> findByBookingTurfOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
