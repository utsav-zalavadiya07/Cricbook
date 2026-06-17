package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TournamentReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRewardRepository extends JpaRepository<TournamentReward, Long> {
    List<TournamentReward> findByTournamentIdOrderByPositionAscCreatedAtAsc(Long tournamentId);
}
