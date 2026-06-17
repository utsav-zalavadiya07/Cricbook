package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    @Query("SELECT r FROM Refund r WHERE r.payment.booking.turf.owner.id = :ownerId ORDER BY r.createdAt DESC")
    List<Refund> findByPaymentBookingTurfOwnerIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("ownerId") Long ownerId);
}
