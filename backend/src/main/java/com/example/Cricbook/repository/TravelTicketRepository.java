package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelTicket;
import com.example.Cricbook.enums.TravelTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TravelTicketRepository extends JpaRepository<TravelTicket, Long> {
    Optional<TravelTicket> findByQrCode(String qrCode);
    List<TravelTicket> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByStatus(TravelTicketStatus status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TravelTicket t WHERE t.status <> :cancelled")
    BigDecimal ticketRevenueExcluding(@Param("cancelled") TravelTicketStatus cancelled);
}
