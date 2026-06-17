package com.example.Cricbook.repository;

import com.example.Cricbook.entity.PublicMatch;
import com.example.Cricbook.enums.PublicMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicMatchRepository extends JpaRepository<PublicMatch, Long> {
    List<PublicMatch> findByStatusOrderByMatchDateAscStartTimeAsc(PublicMatchStatus status);
    List<PublicMatch> findByCreatorIdOrderByMatchDateDescStartTimeDesc(Long creatorId);
    List<PublicMatch> findByStatusInOrderByMatchDateAscStartTimeAsc(List<PublicMatchStatus> statuses);
    List<PublicMatch> findByHomeTeamIdOrAwayTeamIdOrderByMatchDateDescStartTimeDesc(Long homeTeamId, Long awayTeamId);
    boolean existsByHomeTeamIdAndAwayTeamIdAndStatusIn(Long homeTeamId, Long awayTeamId, List<PublicMatchStatus> statuses);
    boolean existsByHomeTeamIdAndAwayTeamIdAndMatchDateAndStatusIn(Long homeTeamId, Long awayTeamId, java.time.LocalDate matchDate, List<PublicMatchStatus> statuses);
}
