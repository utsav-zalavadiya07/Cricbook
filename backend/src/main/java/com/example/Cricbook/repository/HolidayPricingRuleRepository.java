package com.example.Cricbook.repository;

import com.example.Cricbook.entity.HolidayPricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HolidayPricingRuleRepository extends JpaRepository<HolidayPricingRule, Long> {
    Optional<HolidayPricingRule> findByTurfId(Long turfId);
}
