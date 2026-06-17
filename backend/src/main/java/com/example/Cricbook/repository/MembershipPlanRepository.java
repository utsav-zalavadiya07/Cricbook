package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MembershipPlan;
import com.example.Cricbook.enums.MembershipPlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    List<MembershipPlan> findByTypeOrderByPriceAsc(MembershipPlanType type);
    List<MembershipPlan> findAllByOrderByPriceAsc();
}
