package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PaymentSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentSettlementRepository extends JpaRepository<PaymentSettlement, Long> {
    List<PaymentSettlement> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
    List<PaymentSettlement> findByUserIdOrderByCreatedAtDesc(Long userId);
}
