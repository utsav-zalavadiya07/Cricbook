package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SupportChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, Long> {
    List<SupportChatMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    List<SupportChatMessage> findTop50ByOrderByCreatedAtDesc();
}
