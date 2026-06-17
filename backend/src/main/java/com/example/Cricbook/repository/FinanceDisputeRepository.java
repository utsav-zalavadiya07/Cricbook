package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FinanceDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceDisputeRepository extends JpaRepository<FinanceDispute, Long> {
    List<FinanceDispute> findByStatusOrderByCreatedAtDesc(String status);
}
