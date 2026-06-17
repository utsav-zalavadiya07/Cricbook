package com.example.Cricbook.repository;

import com.example.Cricbook.entity.FanClubMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FanClubMembershipRepository extends JpaRepository<FanClubMembership, Long> {
    Optional<FanClubMembership> findByFanProfileIdAndFanClubIdAndStatus(Long fanProfileId, Long fanClubId, String status);

    long countByFanClubIdAndStatus(Long fanClubId, String status);

    List<FanClubMembership> findAllByOrderByJoinedAtDesc();
}
