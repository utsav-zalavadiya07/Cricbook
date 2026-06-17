package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SlotPriceOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotPriceOverrideRepository extends JpaRepository<SlotPriceOverride, Long> {
    Optional<SlotPriceOverride> findBySlotIdAndDate(Long slotId, LocalDate date);
    List<SlotPriceOverride> findBySlotTurfIdAndDate(Long turfId, LocalDate date);
}
