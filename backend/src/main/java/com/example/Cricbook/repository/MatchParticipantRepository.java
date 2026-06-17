package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
    List<MatchParticipant> findByMatchIdOrderByJoinedAtAsc(Long matchId);
    List<MatchParticipant> findByUserIdOrderByJoinedAtDesc(Long userId);
    Optional<MatchParticipant> findByMatchIdAndUserId(Long matchId, Long userId);
    boolean existsByMatchIdAndUserId(Long matchId, Long userId);
    long countByMatchId(Long matchId);
}
