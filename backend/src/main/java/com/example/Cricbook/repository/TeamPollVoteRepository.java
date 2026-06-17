package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamPollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamPollVoteRepository extends JpaRepository<TeamPollVote, Long> {
    Optional<TeamPollVote> findByPollIdAndUserId(Long pollId, Long userId);
    List<TeamPollVote> findByPollId(Long pollId);
}
