package com.example.Cricbook.repository;

import com.example.Cricbook.entity.ScoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoutProfileRepository extends JpaRepository<ScoutProfile, Long> {
    boolean existsByEmailIgnoreCase(String email);
    List<ScoutProfile> findAllByOrderByCreatedAtDesc();
    List<ScoutProfile> findByStatusOrderByCreatedAtDesc(String status);
}
