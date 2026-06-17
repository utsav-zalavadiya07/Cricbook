package com.example.Cricbook.repository;

import com.example.Cricbook.entity.LandingPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LandingPageRepository extends JpaRepository<LandingPage, Long> {
    Optional<LandingPage> findBySlug(String slug);
    List<LandingPage> findAllByOrderByUpdatedAtDesc();
}
