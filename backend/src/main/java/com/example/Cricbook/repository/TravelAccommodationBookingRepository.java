package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelAccommodationBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TravelAccommodationBookingRepository extends JpaRepository<TravelAccommodationBooking, Long> {
    List<TravelAccommodationBooking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(b.total), 0) FROM TravelAccommodationBooking b WHERE b.status <> 'CANCELLED'")
    BigDecimal accommodationRevenue();
}
