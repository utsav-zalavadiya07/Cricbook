package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsVolunteerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SportsVolunteerProfileRepository extends JpaRepository<SportsVolunteerProfile, Long> {
    boolean existsByVolunteerCode(String volunteerCode);

    boolean existsByEmailIgnoreCase(String email);

    List<SportsVolunteerProfile> findAllByOrderByCreatedAtDesc();
}
