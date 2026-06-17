package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerRecommendationLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerRecommendationLetterRepository extends JpaRepository<CareerRecommendationLetter, Long> {
    List<CareerRecommendationLetter> findByProfileIdOrderByCreatedAtDesc(Long profileId);
    List<CareerRecommendationLetter> findByStatusOrderByCreatedAtDesc(String status);
    List<CareerRecommendationLetter> findAllByOrderByCreatedAtDesc();
}
