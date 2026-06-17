package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ReviewModeration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewModerationRepository extends JpaRepository<ReviewModeration, Long> {
    List<ReviewModeration> findTop50ByOrderByCreatedAtDesc();
}
