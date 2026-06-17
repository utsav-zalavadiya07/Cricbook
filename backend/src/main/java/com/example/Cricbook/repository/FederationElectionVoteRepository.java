package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationElectionVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FederationElectionVoteRepository extends JpaRepository<FederationElectionVote, Long> {
    boolean existsByElectionIdAndVoterReference(Long electionId, String voterReference);
    List<FederationElectionVote> findByElectionIdOrderByCreatedAtDesc(Long electionId);
}
