package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TrustAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrustAdjustmentRepository extends JpaRepository<TrustAdjustment, Long> {
    List<TrustAdjustment> findTop50ByOrderByCreatedAtDesc();
}
