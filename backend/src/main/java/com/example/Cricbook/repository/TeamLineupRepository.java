package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamLineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamLineupRepository extends JpaRepository<TeamLineup, Long> {
    Optional<TeamLineup> findByTeamIdAndMatchIdAndUserId(Long teamId, Long matchId, Long userId);
    List<TeamLineup> findByTeamIdAndMatchIdOrderByPositionAsc(Long teamId, Long matchId);
    List<TeamLineup> findByTeamIdOrderByCreatedAtDesc(Long teamId);
}
