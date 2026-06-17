package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CoachProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachProfileRepository extends JpaRepository<CoachProfile, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<CoachProfile> findByEmailIgnoreCase(String email);
    List<CoachProfile> findAllByOrderByCreatedAtDesc();
    List<CoachProfile> findByStatusOrderByCreatedAtDesc(String status);
}
