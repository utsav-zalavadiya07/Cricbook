package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateTeamRepository extends JpaRepository<CorporateTeam, Long> {
    List<CorporateTeam> findAllByOrderByCreatedAtDesc();
    List<CorporateTeam> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
