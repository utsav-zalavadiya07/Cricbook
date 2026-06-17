package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<Payment> findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(Long bookingId, List<String> statuses);

    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByBookingUserIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("userId") Long userId);

    @Query("SELECT p FROM Payment p WHERE p.booking.turf.owner.id = :ownerId ORDER BY p.createdAt DESC")
    List<Payment> findByBookingTurfOwnerIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("ownerId") Long ownerId);
}
