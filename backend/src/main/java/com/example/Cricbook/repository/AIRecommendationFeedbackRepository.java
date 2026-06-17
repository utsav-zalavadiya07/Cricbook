package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AIRecommendationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AIRecommendationFeedbackRepository extends JpaRepository<AIRecommendationFeedback, Long> {
    List<AIRecommendationFeedback> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByFeedbackType(String feedbackType);
}
