package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamPoll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamPollRepository extends JpaRepository<TeamPoll, Long> {
    List<TeamPoll> findByTeamIdOrderByCreatedAtDesc(Long teamId);
}
