package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SlotAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotAvailabilityRepository extends JpaRepository<SlotAvailability, Long> {
    List<SlotAvailability> findBySlotTurfIdAndDate(Long turfId, LocalDate date);
    Optional<SlotAvailability> findBySlotIdAndDate(Long slotId, LocalDate date);
    List<SlotAvailability> findBySlotTurfIdAndDateBetween(Long turfId, LocalDate startDate, LocalDate endDate);

    @org.springframework.data.jpa.repository.Query("""
        SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END
        FROM SlotAvailability sa
        JOIN sa.slot s
        WHERE s.turf.id = :turfId
        AND sa.date = :date
        AND s.startTime = :time
        AND sa.isAvailable = true
        AND sa.isBlocked = false
    """)
    boolean existsAvailableSlot(
            @org.springframework.data.repository.query.Param("turfId") Long turfId, 
            @org.springframework.data.repository.query.Param("date") LocalDate date, 
            @org.springframework.data.repository.query.Param("time") java.time.LocalTime time);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT sa FROM SlotAvailability sa WHERE sa.slot.id = :slotId AND sa.date = :date")
    Optional<SlotAvailability> lockSlot(@org.springframework.data.repository.query.Param("slotId") Long slotId, @org.springframework.data.repository.query.Param("date") LocalDate date);
}
