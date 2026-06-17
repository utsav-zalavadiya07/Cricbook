package com.example.Cricbook.repository;

import com.example.Cricbook.entity.GovernmentGrantDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GovernmentGrantDisbursementRepository extends JpaRepository<GovernmentGrantDisbursement, Long> {
    List<GovernmentGrantDisbursement> findAllByOrderByCreatedAtDesc();

    List<GovernmentGrantDisbursement> findByGrantApplicationIdOrderByCreatedAtDesc(Long grantApplicationId);
}
