package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AutoWaitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutoWaitlistRepository extends JpaRepository<AutoWaitlist, Long> {
    List<AutoWaitlist> findByUserIdOrderByCreatedAtDesc(Long userId);
}
