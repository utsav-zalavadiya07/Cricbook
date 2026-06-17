package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SupportAutomation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportAutomationRepository extends JpaRepository<SupportAutomation, Long> {
    List<SupportAutomation> findAllByOrderByCreatedAtDesc();
}
