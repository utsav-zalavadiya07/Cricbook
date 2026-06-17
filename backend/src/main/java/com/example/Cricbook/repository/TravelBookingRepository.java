package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelBooking;
import com.example.Cricbook.enums.TravelBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TravelBookingRepository extends JpaRepository<TravelBooking, Long> {
    List<TravelBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TravelBooking> findTop20ByOrderByCreatedAtDesc();
    long countByStatus(TravelBookingStatus status);

    @Query("SELECT COALESCE(SUM(b.total), 0) FROM TravelBooking b WHERE b.status <> :cancelled")
    BigDecimal totalRevenueExcluding(@Param("cancelled") TravelBookingStatus cancelled);
}
