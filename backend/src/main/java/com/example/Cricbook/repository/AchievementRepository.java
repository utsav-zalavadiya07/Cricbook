package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Achievement;
import com.example.Cricbook.enums.GamificationActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByName(String name);
    List<Achievement> findByActiveTrueOrderByThresholdAsc();
    List<Achievement> findByTriggerTypeAndActiveTrueOrderByThresholdAsc(GamificationActivityType triggerType);
}
