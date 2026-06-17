package com.example.Cricbook.repository;

import com.example.Cricbook.entity.MediaReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaReviewRepository extends JpaRepository<MediaReview, Long> {
    List<MediaReview> findTop50ByOrderByCreatedAtDesc();
}
