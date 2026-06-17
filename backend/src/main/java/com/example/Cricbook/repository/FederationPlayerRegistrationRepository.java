package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FederationPlayerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FederationPlayerRegistrationRepository extends JpaRepository<FederationPlayerRegistration, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsBySportsId(String sportsId);
    Optional<FederationPlayerRegistration> findBySportsId(String sportsId);
    List<FederationPlayerRegistration> findByStatusOrderByRankingScoreDescCreatedAtDesc(String status);
    List<FederationPlayerRegistration> findAllByOrderByCreatedAtDesc();
}
