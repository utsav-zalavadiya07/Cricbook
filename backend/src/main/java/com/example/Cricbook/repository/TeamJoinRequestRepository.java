package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamJoinRequest;
import com.example.Cricbook.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamJoinRequestRepository extends JpaRepository<TeamJoinRequest, Long> {
    boolean existsByTeamIdAndRequesterIdAndStatus(Long teamId, Long requesterId, InviteStatus status);
    Optional<TeamJoinRequest> findByIdAndRequesterId(Long id, Long requesterId);
    List<TeamJoinRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
    List<TeamJoinRequest> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    List<TeamJoinRequest> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, InviteStatus status);
}
