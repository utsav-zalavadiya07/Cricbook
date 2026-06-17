package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AIConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIConversationMessageRepository extends JpaRepository<AIConversationMessage, Long> {
    List<AIConversationMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<AIConversationMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByIntent(String intent);
}
