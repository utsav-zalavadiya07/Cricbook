package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RecurringCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringCollectionRepository extends JpaRepository<RecurringCollection, Long> {
    List<RecurringCollection> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<RecurringCollection> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
}
