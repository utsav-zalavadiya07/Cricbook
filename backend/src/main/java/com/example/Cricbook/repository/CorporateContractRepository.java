package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateContractRepository extends JpaRepository<CorporateContract, Long> {
    List<CorporateContract> findAllByOrderByCreatedAtDesc();
    List<CorporateContract> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
