package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ModerationAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationAppealRepository extends JpaRepository<ModerationAppeal, Long> {
    List<ModerationAppeal> findByStatusOrderByCreatedAtDesc(String status);
    List<ModerationAppeal> findTop50ByOrderByCreatedAtDesc();
}
