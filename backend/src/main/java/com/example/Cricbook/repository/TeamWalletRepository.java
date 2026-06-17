package com.example.Cricbook.repository;

import com.example.Cricbook.entity.TeamWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamWalletRepository extends JpaRepository<TeamWallet, Long> {
    Optional<TeamWallet> findByTeamId(Long teamId);
}
