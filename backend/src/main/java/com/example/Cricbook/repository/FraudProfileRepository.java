package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FraudProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudProfileRepository extends JpaRepository<FraudProfile, Long> {
    List<FraudProfile> findByStatusOrderByCreatedAtDesc(String status);
    List<FraudProfile> findTop50ByOrderByCreatedAtDesc();
    boolean existsByUserIdAndStatus(Long userId, String status);
}
