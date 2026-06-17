package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamModeration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamModerationRepository extends JpaRepository<TeamModeration, Long> {
    List<TeamModeration> findTop50ByOrderByCreatedAtDesc();
}
