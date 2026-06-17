package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FeedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedEventRepository extends JpaRepository<FeedEvent, Long> {
    List<FeedEvent> findTop25ByOrderByCreatedAtDesc();
    List<FeedEvent> findTop25ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);
    List<FeedEvent> findTop50ByStatusOrderByCreatedAtDesc(String status);
    List<FeedEvent> findTop50ByStatusOrderByEngagementScoreDescCreatedAtDesc(String status);

    @Query("""
            SELECT f FROM FeedEvent f
            WHERE f.status = 'ACTIVE'
            AND (:scope IS NULL OR f.visibility = :scope OR f.visibility = 'PUBLIC')
            AND (:query IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(f.summary) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(f.entityType) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY f.createdAt DESC
            """)
    List<FeedEvent> searchActiveFeed(@Param("scope") String scope, @Param("query") String query);

    long countByStatus(String status);
}
