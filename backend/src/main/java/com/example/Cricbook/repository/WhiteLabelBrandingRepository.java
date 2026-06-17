package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WhiteLabelBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhiteLabelBrandingRepository extends JpaRepository<WhiteLabelBranding, Long> {
    Optional<WhiteLabelBranding> findByTenantId(Long tenantId);
}
