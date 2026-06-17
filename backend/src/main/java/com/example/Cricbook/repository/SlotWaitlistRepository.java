package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SlotWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotWaitlistRepository extends JpaRepository<SlotWaitlist, Long> {
    Optional<SlotWaitlist> findBySlotIdAndUserIdAndDate(Long slotId, Long userId, LocalDate date);
    List<SlotWaitlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<SlotWaitlist> findBySlotIdAndDateAndActiveTrueOrderByPriorityDescCreatedAtAsc(Long slotId, LocalDate date);
    long countByActiveTrue();
}
