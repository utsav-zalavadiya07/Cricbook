package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamAttendance;
import com.example.Cricbook.enums.TeamAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamAttendanceRepository extends JpaRepository<TeamAttendance, Long> {
    Optional<TeamAttendance> findByTeamIdAndMatchIdAndUserId(Long teamId, Long matchId, Long userId);
    List<TeamAttendance> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    long countByTeamIdAndStatus(Long teamId, TeamAttendanceStatus status);
    long countByTeamId(Long teamId);
}
