package com.example.Cricbook.repository;

import com.example.Cricbook.entity.UniversalWalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversalWalletAccountRepository extends JpaRepository<UniversalWalletAccount, Long> {
    Optional<UniversalWalletAccount> findByIdentityId(Long identityId);

    List<UniversalWalletAccount> findAllByOrderByCreatedAtDesc();
}
