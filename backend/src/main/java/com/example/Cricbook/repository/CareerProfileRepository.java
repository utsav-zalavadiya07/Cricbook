package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareerProfileRepository extends JpaRepository<CareerProfile, Long> {
    boolean existsByPlayerEmailIgnoreCase(String playerEmail);
    Optional<CareerProfile> findByPlayerEmailIgnoreCase(String playerEmail);
    Optional<CareerProfile> findByShareCode(String shareCode);
    List<CareerProfile> findAllByOrderByCreatedAtDesc();
    List<CareerProfile> findByStatusOrderByRankingScoreDescCreatedAtDesc(String status);
}
