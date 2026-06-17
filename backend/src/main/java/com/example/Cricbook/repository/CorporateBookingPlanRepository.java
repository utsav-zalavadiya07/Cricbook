package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateBookingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateBookingPlanRepository extends JpaRepository<CorporateBookingPlan, Long> {
    List<CorporateBookingPlan> findAllByOrderByCreatedAtDesc();
    List<CorporateBookingPlan> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
