package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsTimelineEventRepository extends JpaRepository<SportsTimelineEvent, Long> {
    List<SportsTimelineEvent> findByIdentityIdOrderByOccurredAtDesc(Long identityId);

    List<SportsTimelineEvent> findAllByOrderByOccurredAtDesc();
}
