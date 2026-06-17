package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CommunityModeration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityModerationRepository extends JpaRepository<CommunityModeration, Long> {
    List<CommunityModeration> findTop50ByOrderByCreatedAtDesc();
}
