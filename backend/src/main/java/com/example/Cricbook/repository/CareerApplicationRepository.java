package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareerApplicationRepository extends JpaRepository<CareerApplication, Long> {
    Optional<CareerApplication> findByOpportunityIdAndProfileId(Long opportunityId, Long profileId);
    List<CareerApplication> findByOpportunityIdOrderByAppliedAtDesc(Long opportunityId);
    List<CareerApplication> findByProfileIdOrderByAppliedAtDesc(Long profileId);
    List<CareerApplication> findAllByOrderByAppliedAtDesc();
    long countByStatus(String status);
}
