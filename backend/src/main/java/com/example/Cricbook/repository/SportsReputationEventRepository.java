package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsReputationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsReputationEventRepository extends JpaRepository<SportsReputationEvent, Long> {
    List<SportsReputationEvent> findByIdentityIdOrderByCreatedAtDesc(Long identityId);

    List<SportsReputationEvent> findAllByOrderByCreatedAtDesc();
}
