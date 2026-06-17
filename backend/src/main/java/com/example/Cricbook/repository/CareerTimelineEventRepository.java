package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CareerTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareerTimelineEventRepository extends JpaRepository<CareerTimelineEvent, Long> {
    List<CareerTimelineEvent> findByProfileIdOrderByEventDateDescCreatedAtDesc(Long profileId);
}
