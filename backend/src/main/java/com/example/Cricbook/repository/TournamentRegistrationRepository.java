package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TournamentRegistration;
import com.example.Cricbook.enums.TournamentRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {
    List<TournamentRegistration> findByTournamentIdOrderByCreatedAtAsc(Long tournamentId);
    List<TournamentRegistration> findByTournamentIdAndStatusOrderByCreatedAtAsc(Long tournamentId, TournamentRegistrationStatus status);
    Optional<TournamentRegistration> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);
    Optional<TournamentRegistration> findByIdAndTournamentId(Long id, Long tournamentId);
    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);
    long countByTournamentIdAndStatus(Long tournamentId, TournamentRegistrationStatus status);
}
