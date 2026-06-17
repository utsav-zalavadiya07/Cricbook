package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsorLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorLeadRepository extends JpaRepository<SponsorLead, Long> {
    List<SponsorLead> findAllByOrderByCreatedAtDesc();
    List<SponsorLead> findByStatusOrderByCreatedAtDesc(String status);
}
