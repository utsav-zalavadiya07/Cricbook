package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    List<PricingRule> findByTurfId(Long turfId);
    Optional<PricingRule> findByTurfIdAndRuleName(Long turfId, String ruleName);
}
