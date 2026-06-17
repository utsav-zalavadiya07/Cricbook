package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TalentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TalentProfileRepository extends JpaRepository<TalentProfile, Long> {
    List<TalentProfile> findAllByOrderByTalentScoreDescCreatedAtDesc();
    List<TalentProfile> findByStatusOrderByTalentScoreDescCreatedAtDesc(String status);
}
