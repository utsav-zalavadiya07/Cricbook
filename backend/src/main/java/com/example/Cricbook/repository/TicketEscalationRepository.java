package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TicketEscalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketEscalationRepository extends JpaRepository<TicketEscalation, Long> {
    List<TicketEscalation> findByStatusOrderByCreatedAtDesc(String status);
    List<TicketEscalation> findTop50ByOrderByCreatedAtDesc();
}
