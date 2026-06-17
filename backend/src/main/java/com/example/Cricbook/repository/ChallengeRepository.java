package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    Optional<Challenge> findByTitle(String title);
    List<Challenge> findByActiveTrueOrderByExpiresAtAsc();
}
