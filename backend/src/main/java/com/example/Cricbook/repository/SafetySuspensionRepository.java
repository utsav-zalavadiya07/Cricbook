package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SafetySuspension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetySuspensionRepository extends JpaRepository<SafetySuspension, Long> {
    List<SafetySuspension> findByStatusOrderByCreatedAtDesc(String status);
    List<SafetySuspension> findTop50ByOrderByCreatedAtDesc();
}
