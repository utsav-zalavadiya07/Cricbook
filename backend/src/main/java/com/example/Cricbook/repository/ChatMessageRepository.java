package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ChatMessage;
import com.example.Cricbook.enums.ChatRoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByRoomTypeAndRoomIdOrderByCreatedAtAsc(ChatRoomType roomType, Long roomId);
}
