package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {
    List<FeedComment> findTop50ByFeedEventIdAndStatusOrderByCreatedAtAsc(Long feedEventId, String status);
    long countByFeedEventIdAndStatus(Long feedEventId, String status);
}
