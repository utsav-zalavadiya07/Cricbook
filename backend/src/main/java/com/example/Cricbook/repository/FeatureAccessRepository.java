package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FeatureAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureAccessRepository extends JpaRepository<FeatureAccess, Long> {

    List<FeatureAccess> findByPlanName(String planName);

    boolean existsByPlanNameAndFeature(String planName, String feature);
}
