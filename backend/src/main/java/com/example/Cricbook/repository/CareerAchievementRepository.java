package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerAchievementRepository extends JpaRepository<CareerAchievement, Long> {
    List<CareerAchievement> findByProfileIdOrderByCreatedAtDesc(Long profileId);
    List<CareerAchievement> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
