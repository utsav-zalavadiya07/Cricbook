package com.example.Cricbook.repository;

import com.example.Cricbook.entity.EnterpriseCenter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnterpriseCenterRepository extends JpaRepository<EnterpriseCenter, Long> {
    List<EnterpriseCenter> findByBusinessId(Long businessId);
    List<EnterpriseCenter> findByBusinessOwnerId(Long ownerId);
    Optional<EnterpriseCenter> findByBusinessIdAndNameIgnoreCaseAndCityIgnoreCase(Long businessId, String name, String city);
}
