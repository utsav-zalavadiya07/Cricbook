package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UniversalRewardAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversalRewardAccountRepository extends JpaRepository<UniversalRewardAccount, Long> {
    Optional<UniversalRewardAccount> findByIdentityId(Long identityId);

    List<UniversalRewardAccount> findAllByOrderByCreatedAtDesc();
}
