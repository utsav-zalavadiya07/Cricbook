package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateLeague;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorporateLeagueRepository extends JpaRepository<CorporateLeague, Long> {
    List<CorporateLeague> findAllByOrderByCreatedAtDesc();
}
