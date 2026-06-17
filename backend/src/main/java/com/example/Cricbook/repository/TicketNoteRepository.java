package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TicketNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketNoteRepository extends JpaRepository<TicketNote, Long> {
    List<TicketNote> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    List<TicketNote> findTop50ByOrderByCreatedAtDesc();
}
