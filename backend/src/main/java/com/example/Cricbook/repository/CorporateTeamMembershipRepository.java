package com.example.Cricbook.repository;

import com.example.Cricbook.entity.CorporateTeamMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorporateTeamMembershipRepository extends JpaRepository<CorporateTeamMembership, Long> {
    List<CorporateTeamMembership> findAllByOrderByCreatedAtDesc();
    List<CorporateTeamMembership> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    Optional<CorporateTeamMembership> findByTeamIdAndEmployeeId(Long teamId, Long employeeId);
}
