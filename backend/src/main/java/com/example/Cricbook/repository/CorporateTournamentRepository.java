package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateTournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateTournamentRepository extends JpaRepository<CorporateTournament, Long> {
    List<CorporateTournament> findAllByOrderByCreatedAtDesc();
    List<CorporateTournament> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
