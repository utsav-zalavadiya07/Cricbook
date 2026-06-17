package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamWalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamWalletTransactionRepository extends JpaRepository<TeamWalletTransaction, Long> {
    List<TeamWalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
