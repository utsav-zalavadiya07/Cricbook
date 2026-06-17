package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SeasonalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SeasonalEventRepository extends JpaRepository<SeasonalEvent, Long> {
    List<SeasonalEvent> findByActiveTrueAndEndDateGreaterThanEqualOrderByStartDateAsc(LocalDate date);
}
