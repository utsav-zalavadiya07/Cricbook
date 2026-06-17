package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamInvite;
import com.example.Cricbook.enums.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    List<TeamInvite> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    List<TeamInvite> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    Optional<TeamInvite> findByIdAndReceiverId(Long id, Long receiverId);
    boolean existsByTeamIdAndReceiverIdAndStatus(Long teamId, Long receiverId, InviteStatus status);
}
