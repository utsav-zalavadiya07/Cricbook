package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SponsorshipContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorshipContractRepository extends JpaRepository<SponsorshipContract, Long> {
    boolean existsByContractNumber(String contractNumber);
    List<SponsorshipContract> findAllByOrderByCreatedAtDesc();
    List<SponsorshipContract> findByStatusOrderByCreatedAtDesc(String status);
}
