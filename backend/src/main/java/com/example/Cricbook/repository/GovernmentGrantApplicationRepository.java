package com.example.Cricbook.repository;

import com.example.Cricbook.entity.GovernmentGrantApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovernmentGrantApplicationRepository extends JpaRepository<GovernmentGrantApplication, Long> {
    boolean existsByGrantCode(String grantCode);

    List<GovernmentGrantApplication> findAllByOrderByCreatedAtDesc();

    List<GovernmentGrantApplication> findByStatusOrderByCreatedAtDesc(String status);
}
