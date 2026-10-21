package com.example.Cricbook.repository;

import com.example.Cricbook.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    Optional<WalletTransaction> findFirstByWalletIdAndReferenceOrderByCreatedAtDesc(Long walletId, String reference);

    Optional<WalletTransaction> findFirstByReferenceOrderByCreatedAtDesc(String reference);
}

