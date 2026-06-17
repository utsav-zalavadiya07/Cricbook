package com.example.Cricbook.repository;

import com.example.Cricbook.entity.OwnerSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnerSettlementRepository extends JpaRepository<OwnerSettlement, Long> {
    List<OwnerSettlement> findByStatusOrderByCreatedAtDesc(String status);
    List<OwnerSettlement> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<OwnerSettlement> findByBookingId(Long bookingId);
}
