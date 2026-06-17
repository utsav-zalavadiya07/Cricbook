package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Long> {
    List<CommissionRule> findByActiveTrueOrderByCreatedAtDesc();
}
