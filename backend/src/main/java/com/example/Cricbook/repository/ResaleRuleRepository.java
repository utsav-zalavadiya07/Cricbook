package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ResaleRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResaleRuleRepository extends JpaRepository<ResaleRule, Long> {
    Optional<ResaleRule> findByOwnerId(Long ownerId);
}
