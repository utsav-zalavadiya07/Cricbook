package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerOpportunityRepository extends JpaRepository<CareerOpportunity, Long> {
    List<CareerOpportunity> findAllByOrderByCreatedAtDesc();
    List<CareerOpportunity> findByStatusOrderByCreatedAtDesc(String status);
    List<CareerOpportunity> findByOpportunityTypeAndStatusOrderByCreatedAtDesc(String opportunityType, String status);
}
