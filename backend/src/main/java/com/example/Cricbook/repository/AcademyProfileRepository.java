package com.example.Cricbook.repository;

import com.example.Cricbook.entity.AcademyProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademyProfileRepository extends JpaRepository<AcademyProfile, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<AcademyProfile> findByNameIgnoreCase(String name);
    List<AcademyProfile> findAllByOrderByCreatedAtDesc();
    List<AcademyProfile> findByStatusOrderByCreatedAtDesc(String status);
}
