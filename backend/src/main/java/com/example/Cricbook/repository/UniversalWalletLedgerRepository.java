package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UniversalWalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UniversalWalletLedgerRepository extends JpaRepository<UniversalWalletLedger, Long> {
    List<UniversalWalletLedger> findByIdentityIdOrderByCreatedAtDesc(Long identityId);

    List<UniversalWalletLedger> findAllByOrderByCreatedAtDesc();
}
