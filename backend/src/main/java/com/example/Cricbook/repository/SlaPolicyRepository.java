package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByPriority(String priority);
}
