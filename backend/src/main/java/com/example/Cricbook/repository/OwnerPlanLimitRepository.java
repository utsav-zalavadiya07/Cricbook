package com.example.Cricbook.repository;

import com.example.Cricbook.entity.OwnerPlanLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnerPlanLimitRepository extends JpaRepository<OwnerPlanLimit, Long> {

    Optional<OwnerPlanLimit> findByPlanName(String planName);
}
