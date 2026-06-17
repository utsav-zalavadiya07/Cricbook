package com.example.Cricbook.repository;

import com.example.Cricbook.entity.DeveloperApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperApplicationRepository extends JpaRepository<DeveloperApplication, Long> {
    boolean existsByDeveloper_IdAndNameIgnoreCase(Long developerId, String name);
    Optional<DeveloperApplication> findByClientId(String clientId);
    List<DeveloperApplication> findByDeveloper_IdOrderByCreatedAtDesc(Long developerId);
    List<DeveloperApplication> findAllByOrderByCreatedAtDesc();
    List<DeveloperApplication> findByStatusOrderByCreatedAtDesc(String status);
}
