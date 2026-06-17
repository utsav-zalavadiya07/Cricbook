package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RebookHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RebookHistoryRepository extends JpaRepository<RebookHistory, Long> {
    Optional<RebookHistory> findByUserIdAndSourceBookingId(Long userId, Long sourceBookingId);
}
