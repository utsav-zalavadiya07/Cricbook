package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateCompanyRepository extends JpaRepository<CorporateCompany, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<CorporateCompany> findAllByOrderByCreatedAtDesc();
    List<CorporateCompany> findByStatusOrderByCreatedAtDesc(String status);
}
