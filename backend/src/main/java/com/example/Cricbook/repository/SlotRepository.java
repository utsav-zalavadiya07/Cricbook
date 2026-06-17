package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByTurfId(Long turfId);
    List<Slot> findByTurfIdAndArchivedFalse(Long turfId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Slot s
            WHERE s.turf.id = :turfId
            AND s.archived = false
            AND s.isActive = true
            AND s.startTime < :endTime
            AND s.endTime > :startTime
            """)
    boolean existsActiveOverlappingSlot(
            @org.springframework.data.repository.query.Param("turfId") Long turfId,
            @org.springframework.data.repository.query.Param("startTime") LocalTime startTime,
            @org.springframework.data.repository.query.Param("endTime") LocalTime endTime);

    @org.springframework.data.jpa.repository.Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Slot s
            WHERE s.turf.id = :turfId
            AND s.id <> :slotId
            AND s.archived = false
            AND s.isActive = true
            AND s.startTime < :endTime
            AND s.endTime > :startTime
            """)
    boolean existsOverlappingSlotExcludingId(
            @org.springframework.data.repository.query.Param("turfId") Long turfId,
            @org.springframework.data.repository.query.Param("slotId") Long slotId,
            @org.springframework.data.repository.query.Param("startTime") LocalTime startTime,
            @org.springframework.data.repository.query.Param("endTime") LocalTime endTime);
}
