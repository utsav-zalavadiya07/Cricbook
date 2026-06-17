package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamAvailability;
import com.example.Cricbook.enums.TeamAvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamAvailabilityRepository extends JpaRepository<TeamAvailability, Long> {
    Optional<TeamAvailability> findByTeamIdAndUserIdAndDate(Long teamId, Long userId, LocalDate date);
    List<TeamAvailability> findByTeamIdAndDateBetweenOrderByDateAscCreatedAtAsc(Long teamId, LocalDate start, LocalDate end);
    long countByTeamIdAndDateAndStatus(Long teamId, LocalDate date, TeamAvailabilityStatus status);
}
