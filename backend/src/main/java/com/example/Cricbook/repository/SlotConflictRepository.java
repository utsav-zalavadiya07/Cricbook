package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SlotConflict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlotConflictRepository extends JpaRepository<SlotConflict, Long> {
    List<SlotConflict> findByStatusOrderByCreatedAtDesc(String status);
}
