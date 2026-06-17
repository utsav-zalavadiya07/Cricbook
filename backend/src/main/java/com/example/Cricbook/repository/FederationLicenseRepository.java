package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationLicenseRepository extends JpaRepository<FederationLicense, Long> {
    boolean existsByLicenseNumber(String licenseNumber);
    List<FederationLicense> findAllByOrderByCreatedAtDesc();
    List<FederationLicense> findByStatusOrderByCreatedAtDesc(String status);
}
