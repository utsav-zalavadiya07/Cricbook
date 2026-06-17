package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CreatorContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreatorContentRepository extends JpaRepository<CreatorContent, Long> {
    List<CreatorContent> findByCreatorProfileIdOrderByCreatedAtDesc(Long creatorProfileId);

    List<CreatorContent> findAllByOrderByCreatedAtDesc();
}
