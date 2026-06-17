package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamWorkspaceRepository extends JpaRepository<TeamWorkspace, Long> {
    Optional<TeamWorkspace> findByTeamId(Long teamId);
}
