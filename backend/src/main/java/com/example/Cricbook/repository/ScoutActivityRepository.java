package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ScoutActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoutActivityRepository extends JpaRepository<ScoutActivity, Long> {
    List<ScoutActivity> findByScoutIdOrderByCreatedAtDesc(Long scoutId);
    List<ScoutActivity> findByProfileIdOrderByCreatedAtDesc(Long profileId);
    List<ScoutActivity> findAllByOrderByCreatedAtDesc();
}
