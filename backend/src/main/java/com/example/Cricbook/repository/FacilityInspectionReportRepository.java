package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FacilityInspectionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityInspectionReportRepository extends JpaRepository<FacilityInspectionReport, Long> {
    List<FacilityInspectionReport> findAllByOrderByCreatedAtDesc();

    List<FacilityInspectionReport> findByFacilityIdOrderByCreatedAtDesc(Long facilityId);

    List<FacilityInspectionReport> findByStatusOrderByCreatedAtDesc(String status);
}
