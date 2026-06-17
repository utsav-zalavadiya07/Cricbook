package com.example.Cricbook.repository;

import com.example.Cricbook.entity.SportsIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SportsIdentityRepository extends JpaRepository<SportsIdentity, Long> {
    boolean existsBySportsId(String sportsId);

    boolean existsByPrimaryEmailIgnoreCase(String primaryEmail);

    Optional<SportsIdentity> findBySportsId(String sportsId);

    Optional<SportsIdentity> findByPrimaryEmailIgnoreCase(String primaryEmail);

    List<SportsIdentity> findByStatusOrderByCreatedAtDesc(String status);

    List<SportsIdentity> findAllByOrderByCreatedAtDesc();

    List<SportsIdentity> findByDisplayNameContainingIgnoreCaseOrPrimaryEmailContainingIgnoreCaseOrSportsIdContainingIgnoreCaseOrderByCreatedAtDesc(
            String displayName,
            String primaryEmail,
            String sportsId
    );
}
