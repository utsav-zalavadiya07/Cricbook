package com.example.Cricbook.repository;

import com.example.Cricbook.entity.RevenueExperiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RevenueExperimentRepository extends JpaRepository<RevenueExperiment, Long> {

    Optional<RevenueExperiment> findByExperimentNameAndVariant(String experimentName, String variant);
}
