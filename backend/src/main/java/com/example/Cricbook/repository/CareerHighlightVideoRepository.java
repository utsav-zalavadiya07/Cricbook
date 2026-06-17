package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerHighlightVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerHighlightVideoRepository extends JpaRepository<CareerHighlightVideo, Long> {
    List<CareerHighlightVideo> findByProfileIdOrderByCreatedAtDesc(Long profileId);
    List<CareerHighlightVideo> findAllByOrderByCreatedAtDesc();
}
