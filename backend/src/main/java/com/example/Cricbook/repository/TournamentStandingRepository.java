package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TournamentStanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentStandingRepository extends JpaRepository<TournamentStanding, Long> {
    List<TournamentStanding> findByTournamentIdOrderByPointsDescWinsDescRunsForDesc(Long tournamentId);
    Optional<TournamentStanding> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);
    void deleteByTournamentId(Long tournamentId);
}
