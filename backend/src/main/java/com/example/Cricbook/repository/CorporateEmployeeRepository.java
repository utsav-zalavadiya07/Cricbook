package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorporateEmployeeRepository extends JpaRepository<CorporateEmployee, Long> {
    List<CorporateEmployee> findAllByOrderByCreatedAtDesc();
    List<CorporateEmployee> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    Optional<CorporateEmployee> findByCompanyIdAndEmailIgnoreCase(Long companyId, String email);
}
