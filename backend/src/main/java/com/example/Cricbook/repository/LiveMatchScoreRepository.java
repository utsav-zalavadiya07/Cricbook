package com.example.Cricbook.repository;

import com.example.Cricbook.entity.LiveMatchScore;
import com.example.Cricbook.enums.LiveMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface LiveMatchScoreRepository extends JpaRepository<LiveMatchScore, Long> {
    List<LiveMatchScore> findByStatusOrderByUpdatedAtDesc(LiveMatchStatus status);
    List<LiveMatchScore> findByStatusInOrderByUpdatedAtDesc(Collection<LiveMatchStatus> statuses);
    Optional<LiveMatchScore> findByFixtureId(Long fixtureId);
    Optional<LiveMatchScore> findByPublicMatchId(Long publicMatchId);
}
