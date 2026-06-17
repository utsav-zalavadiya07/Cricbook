package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SupportAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportAgentRepository extends JpaRepository<SupportAgent, Long> {
    List<SupportAgent> findByStatusOrderByCreatedAtDesc(String status);
    List<SupportAgent> findAllByOrderByCreatedAtDesc();
}
