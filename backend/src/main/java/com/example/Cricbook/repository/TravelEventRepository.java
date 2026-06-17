package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TravelEvent;
import com.example.Cricbook.enums.TravelEventStatus;
import com.example.Cricbook.enums.TravelEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelEventRepository extends JpaRepository<TravelEvent, Long> {
    @Query("""
            SELECT e FROM TravelEvent e
            WHERE e.status = :status
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:query IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:sport IS NULL OR LOWER(e.sport) = LOWER(:sport))
              AND (:city IS NULL OR LOWER(e.city) = LOWER(:city))
            ORDER BY e.startsAt ASC
            """)
    List<TravelEvent> searchPublished(
            @Param("status") TravelEventStatus status,
            @Param("eventType") TravelEventType eventType,
            @Param("query") String query,
            @Param("sport") String sport,
            @Param("city") String city);

    List<TravelEvent> findTop8ByStatusOrderByStartsAtAsc(TravelEventStatus status);
}
