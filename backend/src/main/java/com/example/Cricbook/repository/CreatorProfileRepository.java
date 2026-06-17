package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CreatorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, Long> {
    boolean existsByEmailIgnoreCase(String email);

    List<CreatorProfile> findAllByOrderByCreatedAtDesc();
}
