package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FeedReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedReactionRepository extends JpaRepository<FeedReaction, Long> {
    Optional<FeedReaction> findByFeedEventIdAndUserId(Long feedEventId, Long userId);
    long countByFeedEventId(Long feedEventId);
    void deleteByFeedEventIdAndUserId(Long feedEventId, Long userId);
}
