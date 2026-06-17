package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UserMembership;
import com.example.Cricbook.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {
    List<UserMembership> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UserMembership> findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(Long userId, MembershipStatus status, LocalDateTime now);
}
