package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TurfEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TurfEnvironmentRepository extends JpaRepository<TurfEnvironment, Long> {
    Optional<TurfEnvironment> findByTurfId(Long turfId);
}
