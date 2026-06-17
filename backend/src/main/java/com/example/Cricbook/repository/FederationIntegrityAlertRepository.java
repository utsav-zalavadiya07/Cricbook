package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationIntegrityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationIntegrityAlertRepository extends JpaRepository<FederationIntegrityAlert, Long> {
    List<FederationIntegrityAlert> findAllByOrderByCreatedAtDesc();
    List<FederationIntegrityAlert> findByStatusOrderByCreatedAtDesc(String status);
}
