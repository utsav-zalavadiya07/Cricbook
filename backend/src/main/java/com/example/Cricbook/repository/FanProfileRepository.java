package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FanProfileRepository extends JpaRepository<FanProfile, Long> {
    boolean existsByFanCode(String fanCode);

    boolean existsByEmailIgnoreCase(String email);

    Optional<FanProfile> findByEmailIgnoreCase(String email);

    List<FanProfile> findAllByOrderByCreatedAtDesc();

    List<FanProfile> findTop10ByStatusOrderByRankPointsDescFanScoreDesc(String status);
}
