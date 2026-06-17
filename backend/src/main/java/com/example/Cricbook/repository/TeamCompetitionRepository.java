package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamCompetition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamCompetitionRepository extends JpaRepository<TeamCompetition, Long> {
    List<TeamCompetition> findTop10ByOrderByPointsDescUpdatedAtDesc();
    List<TeamCompetition> findBySeasonNameOrderByRankAsc(String seasonName);
}
