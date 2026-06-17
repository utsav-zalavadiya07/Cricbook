package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateWellnessProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateWellnessProgramRepository extends JpaRepository<CorporateWellnessProgram, Long> {
    List<CorporateWellnessProgram> findAllByOrderByCreatedAtDesc();
    List<CorporateWellnessProgram> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
