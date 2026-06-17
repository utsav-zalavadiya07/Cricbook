package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateLocationRepository extends JpaRepository<CorporateLocation, Long> {
    List<CorporateLocation> findAllByOrderByCreatedAtDesc();
    List<CorporateLocation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
