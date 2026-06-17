package com.example.Cricbook.repository;

import com.example.Cricbook.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    List<Referral> findByReferrerId(Long referrerId);

    Optional<Referral> findByInviteeId(Long inviteeId);

    Optional<Referral> findByReferralCodeAndStatus(String referralCode, String status);
}
