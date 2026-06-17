package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateDepartmentRepository extends JpaRepository<CorporateDepartment, Long> {
    List<CorporateDepartment> findAllByOrderByCreatedAtDesc();
    List<CorporateDepartment> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
