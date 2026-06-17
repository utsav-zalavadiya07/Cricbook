package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UniversalRewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversalRewardLedgerRepository extends JpaRepository<UniversalRewardLedger, Long> {
    List<UniversalRewardLedger> findByIdentityIdOrderByCreatedAtDesc(Long identityId);

    List<UniversalRewardLedger> findAllByOrderByCreatedAtDesc();
}
