package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamMember;
import com.example.Cricbook.enums.TeamMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamIdOrderByCreatedAtAsc(Long teamId);
    List<TeamMember> findByTeamIdAndStatusOrderByCreatedAtAsc(Long teamId, TeamMemberStatus status);
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);
    Optional<TeamMember> findByTeamIdAndUserIdAndStatus(Long teamId, Long userId, TeamMemberStatus status);
    boolean existsByTeamIdAndUserId(Long teamId, Long userId);
    boolean existsByTeamIdAndUserIdAndStatus(Long teamId, Long userId, TeamMemberStatus status);
    long countByTeamId(Long teamId);
    long countByTeamIdAndStatus(Long teamId, TeamMemberStatus status);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId ORDER BY tm.createdAt DESC")
    List<TeamMember> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.status = :status ORDER BY tm.createdAt DESC")
    List<TeamMember> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TeamMemberStatus status);
}
