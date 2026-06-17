package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TournamentFixture;
import com.example.Cricbook.enums.FixtureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@Repository
public interface TournamentFixtureRepository extends JpaRepository<TournamentFixture, Long> {
    List<TournamentFixture> findByTournamentIdOrderByRoundAscMatchDateAscStartTimeAsc(Long tournamentId);
    List<TournamentFixture> findByTournamentIdAndRoundOrderByMatchDateAscStartTimeAscIdAsc(Long tournamentId, Integer round);
    boolean existsByTournamentId(Long tournamentId);
    long countByTournamentIdAndRound(Long tournamentId, Integer round);
    boolean existsByTurfIdAndMatchDateAndStartTimeAndStatusInAndIdNot(
            Long turfId,
            LocalDate matchDate,
            LocalTime startTime,
            List<FixtureStatus> statuses,
            Long id
    );
    void deleteByTournamentId(Long tournamentId);
}
