package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanClub;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FanClubRepository extends JpaRepository<FanClub, Long> {
    boolean existsByClubCode(String clubCode);

    boolean existsByNameIgnoreCaseAndSportIgnoreCase(String name, String sport);

    List<FanClub> findAllByOrderByCreatedAtDesc();

    List<FanClub> findByStatusOrderByCreatedAtDesc(String status);
}
