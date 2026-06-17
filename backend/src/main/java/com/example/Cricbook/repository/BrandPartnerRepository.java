package com.example.Cricbook.repository;

import com.example.Cricbook.entity.BrandPartner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandPartnerRepository extends JpaRepository<BrandPartner, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<BrandPartner> findByEmailIgnoreCase(String email);
    List<BrandPartner> findAllByOrderByCreatedAtDesc();
    List<BrandPartner> findByStatusOrderByCreatedAtDesc(String status);
}
