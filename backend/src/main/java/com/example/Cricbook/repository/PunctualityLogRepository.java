package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PunctualityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PunctualityLogRepository extends JpaRepository<PunctualityLog, Long> {
    List<PunctualityLog> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
