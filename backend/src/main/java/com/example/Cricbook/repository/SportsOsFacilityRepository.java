package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsOsFacility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsOsFacilityRepository extends JpaRepository<SportsOsFacility, Long> {
    boolean existsByFacilityCode(String facilityCode);

    boolean existsByNameIgnoreCaseAndDistrictNameIgnoreCase(String name, String districtName);

    List<SportsOsFacility> findAllByOrderByCreatedAtDesc();

    List<SportsOsFacility> findByStatusOrderByCreatedAtDesc(String status);

    List<SportsOsFacility> findByNameContainingIgnoreCaseOrCityNameContainingIgnoreCaseOrDistrictNameContainingIgnoreCaseOrFacilityTypeContainingIgnoreCaseOrderByCreatedAtDesc(
            String name,
            String cityName,
            String districtName,
            String facilityType
    );
}
