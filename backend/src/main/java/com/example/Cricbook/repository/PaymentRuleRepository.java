package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PaymentRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRuleRepository extends JpaRepository<PaymentRule, Long> {
    Optional<PaymentRule> findByOwnerId(Long ownerId);
}
