package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UnifiedMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnifiedMembershipRepository extends JpaRepository<UnifiedMembership, Long> {
    Optional<UnifiedMembership> findFirstByIdentityIdAndStatusOrderByCreatedAtDesc(Long identityId, String status);

    List<UnifiedMembership> findByIdentityIdOrderByCreatedAtDesc(Long identityId);

    List<UnifiedMembership> findAllByOrderByCreatedAtDesc();
}
